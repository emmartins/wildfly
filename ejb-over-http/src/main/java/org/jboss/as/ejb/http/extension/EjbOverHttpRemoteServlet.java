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
package org.jboss.as.ejb.http.extension;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.ejb.http.remote.HttpChannel;
import org.jboss.as.ejb.http.remote.HttpEJBClientMessageReceiver;
import org.jboss.as.ejb.http.remote.HttpMessageInputStream;

/**
 * @author martins
 * @author sfcoy
 */
public class EjbOverHttpRemoteServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final HttpEJBClientMessageReceiver receiver;

    public EjbOverHttpRemoteServlet(HttpEJBClientMessageReceiver receiver) {
        this.receiver = receiver;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        EjbOverHttpLogger.LOGGER.handlingRequestTo(this.getServletName(), request.getRequestURL());
        request.getSession(true);
        response.setContentType("application/octet-stream");
        final AsyncContext asyncContext = request.startAsync();
        final HttpChannel httpChannel = new HttpChannel(asyncContext);
        final HttpMessageInputStream httpMessageInputStream = new HttpMessageInputStream(asyncContext.getRequest().getInputStream());
        receiver.handleMessage(httpChannel, httpMessageInputStream);
    }

}
