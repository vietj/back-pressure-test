package vietj;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FrontClient extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(FrontClient.class.getName());
  }

  private int numClients = 50;
  private NetClient client;
  private int size = 0;
  private Set<BufferReadStream> streams = new HashSet<>();

  @Override
  public void start() throws Exception {
    client = vertx.createNetClient();
    connect();
    vertx.setPeriodic(1000, id -> {
      long totalSent = streams.stream().map(BufferReadStream::sent).<Long>reduce(0L, (a, b) -> a+b);
      System.out.println("sent: " + totalSent + " / " + size);
    });
  }

  private void connect() {
    if (size < numClients) {
      size++;
      client.connect(1234, "localhost", ar -> {
        if (ar.succeeded()) {
          NetSocket so = ar.result();
          BufferReadStream stream = new BufferReadStream(vertx, 16 * 1014);
          streams.add(stream);
          Pump pump = Pump.pump(stream, so);
          pump.start();
          stream.send();
          so.closeHandler(v -> {
            pump.stop();
            streams.remove(stream);
            size--;
            connect();
          });
          connect();
        } else {
          size--;
          ar.cause().printStackTrace();
          connect();
        }
      });
    }
  }
}
