package vietj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BlockingFrontend {

  public static void main(String[] args) throws IOException {
    ExecutorService exec = Executors.newFixedThreadPool(500);
    ServerSocket server = new ServerSocket();
    server.bind(new InetSocketAddress("localhost", 1234));
    while (true) {
      Socket src = server.accept();
      exec.execute(() -> {
        try {
          InputStream in = src.getInputStream();
          OutputStream out = src.getOutputStream();
          byte[] buffer = new byte[256 * 1024];
          while (true) {
            int len = in.read(buffer);
            if (len < 0) {
              break;
            }
            out.write(buffer, 0, len);
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try {
            src.close();
          } catch (IOException ignore) {
          }
        }
      });
    }
  }

}
