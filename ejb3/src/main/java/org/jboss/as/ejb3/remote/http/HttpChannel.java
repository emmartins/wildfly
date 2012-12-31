package org.jboss.as.ejb3.remote.http;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.remoting3.Attachments;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.MessageOutputStream;
import org.xnio.Option;

public class HttpChannel implements Channel {

    private final AsyncContext asyncContext;

    public HttpChannel(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    @Override
    public Attachments getAttachments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void awaitClosed() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void awaitClosedUninterruptibly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeAsync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized org.jboss.remoting3.HandleableCloseable.Key addCloseHandler(CloseHandler<? super Channel> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection() {
        return new HttpConnection((HttpServletRequest) asyncContext.getRequest());
    }

    @Override
    public MessageOutputStream writeMessage() throws IOException {
        return new HttpMessageOutputStream(asyncContext.getResponse().getOutputStream());
    }

    @Override
    public void writeShutdown() throws IOException {
        // do nothing
    }

    @Override
    public void receiveMessage(Receiver handler) {
       // ignore, this channel is used only once by a handler
    }

    @Override
    public boolean supportsOption(Option<?> option) {
        return false;
    }

    @Override
    public <T> T getOption(Option<T> option) {
        return null;
    }

    @Override
    public <T> T setOption(Option<T> option, T value) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        try {
            asyncContext.complete();
        } catch(Throwable e) {
            // ignore
        }
    }

}
