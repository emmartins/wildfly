/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb.http.remote;

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
