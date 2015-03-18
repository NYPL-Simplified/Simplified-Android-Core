package org.nypl.simplified.downloader.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Callable;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.connect.SocketConnection;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

public final class FileServer implements Container, Callable<Unit>
{
  private final InetSocketAddress        address;
  private final SocketConnection         connection;
  private final ContainerSocketProcessor server;

  public FileServer(
    final int port)
    throws IOException
  {
    this.server = new ContainerSocketProcessor(this);
    this.connection = new SocketConnection(this.server);
    this.address = new InetSocketAddress(port);
  }

  @Override public Unit call()
    throws Exception
  {
    final SocketAddress r = this.connection.connect(this.address);
    System.out.println("info: server started on " + r);
    return Unit.unit();
  }

  public void stop()
    throws IOException
  {
    this.connection.close();
    this.server.stop();
  }

  private static byte[] fileAsBytes(
    final String path)
    throws IOException
  {
    final InputStream stream = FileServer.class.getResourceAsStream(path);
    if (stream == null) {
      return null;
    }

    final byte[] buffer = new byte[2 << 14];
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (;;) {
      final int r = stream.read(buffer);
      if (r == -1) {
        break;
      }
      bos.write(buffer, 0, r);
      bos.flush();
    }
    bos.close();
    stream.close();

    return bos.toByteArray();
  }

  @Override public void handle(
    final Request request,
    final Response response)
  {
    try {
      final InetSocketAddress addr = request.getClientAddress();
      System.err.printf(
        "request: %s: %s %s\n",
        addr,
        request.getMethod(),
        request.getTarget());

      final String path = NullCheck.notNull(request.getTarget());
      final byte[] bytes = FileServer.fileAsBytes(path);
      if (bytes == null) {
        response.setStatus(Status.NOT_FOUND);
        response.close();
        return;
      }

      response.setContentLength(bytes.length);
      response.setStatus(Status.OK);

      if ("GET".equals(request.getMethod())) {
        final OutputStream out = response.getOutputStream();
        out.write(bytes);
        out.flush();
        out.close();
      }

    } catch (final IOException e) {
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
    } finally {
      try {
        response.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(
    final String args[])
    throws Exception
  {
    final FileServer s = new FileServer(9999);
    s.call();
  }
}
