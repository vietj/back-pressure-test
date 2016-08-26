package vietj;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

import java.util.Random;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BackendServer extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(BackendServer.class.getName());
  }

  private NetServer server;
  private long threshold = 64000;
  private long minPause = 100;
  private long maxPause = 150;
  private long received;

  @Override
  public void start() throws Exception {
    server = vertx.createNetServer();
    server.connectHandler(so -> {
      Random r = new Random();
      so.exceptionHandler(Throwable::printStackTrace);
      so.handler(buff -> {
        received += buff.length();
        long delay = 0;
        while (received > threshold) {
          received -= threshold;
          delay += minPause + (long) Math.floor(Math.abs(r.nextDouble() * maxPause));
        }
        if (delay > 0) {
          so.pause();
          vertx.setTimer(delay, id -> {
            so.resume();
          });
        }
      });
    });
    server.listen(12345, "localhost");
  }
}
