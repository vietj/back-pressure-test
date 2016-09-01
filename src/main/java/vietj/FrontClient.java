package vietj;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FrontClient extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(FrontClient.class.getName());
  }

  private Random random = new Random();
  private int numClients = 10;
  private NetClient client;
  private int num = 0;
  private int size = 0;
  private Set<Sender> senders = new HashSet<>();

  private long threshold = 64000;
  private long[] pauses = { 10, 50, 100 };

  private long numPauses = 0;
  private long numResumes = 0;

  @Override
  public void start() throws Exception {
    client = vertx.createNetClient();
    connect();
    vertx.setPeriodic(1000, id -> {
      long totalSent = senders.stream().map(Sender::sent).<Long>reduce(0L, (a, b) -> a+b);
      System.out.println("sent: " + totalSent + " / num:" + size + " / pauses:" + numPauses + " / resumes:" + numResumes);
    });
  }

  private void connect() {
    if (num < numClients) {
      num++;
      client.connect(1234, "localhost", ar -> {
        if (ar.succeeded()) {
          NetSocket so = ar.result();
          Sender sender = new Sender(so);
          senders.add(sender);
          sender.start();
          connect();
        } else {
          num--;
          connect();
        }
      });
    }
  }

  private class Sender {

    final long delay = pauses[random.nextInt(pauses.length)];
    final NetSocket socket;
    final Buffer data = Buffer.buffer(new byte[16 * 1024]);
    long received = 0;
    long sentBuffers = 0;
    boolean stopped;

    Sender(NetSocket socket) {
      this.socket = socket;
    }

    void start() {
      size++;

      // Receive part
      socket.exceptionHandler(Throwable::printStackTrace);
      socket.handler(buff -> {
        long pause = 0;
        received += buff.length();
        while (received > threshold) {
          received -= threshold;
          pause += delay;
        }
        if (pause > 0) {
          numPauses++;
          socket.pause();
          vertx.setTimer(pause, id -> {
            numResumes++;
            socket.resume();
          });
        }
      });

      //
      socket.closeHandler(v -> {
        stop();
      });

      send();
    }

    void send() {
      if (!stopped) {
        if (!socket.writeQueueFull()) {
          sentBuffers++;
          socket.write(data);
          context.runOnContext(v -> {
            send();
          });
        } else {
          socket.drainHandler(v -> {
            send();
          });
        }
      }
    }

    private void stop() {
      stopped = true;
      size--;
      senders.remove(this);
      num--;
      connect();
    }

    long sent() {
      long sent = sentBuffers * data.length();
      sentBuffers = 0;
      return sent;
    }
  }
}
