package vietj;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class BufferReadStream implements ReadStream<Buffer> {

  private final Context context;
  private final Buffer data;
  private boolean paused;
  private Handler<Buffer> dataHandler;
  private long sentBuffers = 0;

  BufferReadStream(Vertx vertx, int chunkSize) {
    this.context = vertx.getOrCreateContext();
    this.data = Buffer.buffer(new byte[chunkSize]);
  }

  @Override
  public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public ReadStream<Buffer> handler(Handler<Buffer> handler) {
    dataHandler = handler;
    return this;
  }

  long sent() {
    long sent = sentBuffers * data.length();
    sentBuffers = 0;
    return sent;
  }

  void send() {
    if (!paused) {
      sentBuffers++;
      dataHandler.handle(data);
      context.runOnContext(v -> {
        send();
      });
    }
  }

  @Override
  public ReadStream<Buffer> pause() {
    paused = true;
    return this;
  }

  @Override
  public ReadStream<Buffer> resume() {
    paused = false;
    send();
    return this;
  }

  @Override
  public ReadStream<Buffer> endHandler(Handler<Void> handler) {
    return this;
  }
}
