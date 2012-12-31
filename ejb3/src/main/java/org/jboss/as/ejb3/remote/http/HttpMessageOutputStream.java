package org.jboss.as.ejb3.remote.http;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.remoting3.MessageOutputStream;

public class HttpMessageOutputStream extends MessageOutputStream {

    private final OutputStream outputStream;

    public HttpMessageOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public MessageOutputStream cancel() {
        try {
            close();
        } catch (IOException e) {
            // ignore
        }
        return this;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

}
