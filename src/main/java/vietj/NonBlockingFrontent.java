package vietj;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;

import java.util.Random;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NonBlockingFrontent extends AbstractVerticle {

  private NetServer server;
  private NetClient client;

  @Override
  public void start() throws Exception {
    server = vertx.createNetServer();
    client = vertx.createNetClient();
    server.connectHandler(src -> {
      client.connect(12345, "localhost", ar -> {
        if (ar.succeeded()) {
          NetSocket dst = ar.result();
          Pump pump = Pump.pump(src, dst);
          pump.start();
          dst.closeHandler(v -> {
            pump.stop();
            src.close();
          });
          src.closeHandler(v -> {
            pump.stop();
            dst.close();
          });
        } else {
          src.close();
        }
      });
    });
    server.listen(1234, "localhost");
  }
}
