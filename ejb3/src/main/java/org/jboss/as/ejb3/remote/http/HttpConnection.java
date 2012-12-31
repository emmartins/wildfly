package org.jboss.as.ejb3.remote.http;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.jboss.remoting3.Attachments;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.UserInfo;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

/**
 * A partial impl of jboss remoting {@link Connection}, to expose the user info wrt a http servlet request.
 *
 * @author martins
 *
 */
public class HttpConnection implements Connection {

    private final HttpServletRequest httpServletRequest;

    public HttpConnection(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
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
    public org.jboss.remoting3.HandleableCloseable.Key addCloseHandler(CloseHandler<? super Connection> handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Attachments getAttachments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized Collection<Principal> getPrincipals() {
        final ArrayList<Principal> list = new ArrayList<Principal>();
        final Principal principal = httpServletRequest.getUserPrincipal();
        if (principal != null) {
            list.add(principal);
        }
        return Collections.unmodifiableCollection(list);
    }

    @Override
    public UserInfo getUserInfo() {
        final Principal principal = httpServletRequest.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        final String name = principal.getName();
        if (name == null) {
            return null;
        }
        return new UserInfo() {
            @Override
            public String getUserName() {
                return name;
            }
        };
    }

    @Override
    public IoFuture<Channel> openChannel(String serviceType, OptionMap optionMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteEndpointName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Endpoint getEndpoint() {
        throw new UnsupportedOperationException();
    }

}
