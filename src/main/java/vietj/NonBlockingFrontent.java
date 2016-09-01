package vietj;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.streams.Pump;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NonBlockingFrontent extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(NonBlockingFrontent.class.getName());
  }

  private NetServer server;

  @Override
  public void start() throws Exception {
    server = vertx.createNetServer();
    server.connectHandler(src -> {
      Pump pump = Pump.pump(src, src);
      pump.start();
      src.closeHandler(v -> {
        pump.stop();
      });
    });
    server.listen(1234, "localhost");
  }
}
