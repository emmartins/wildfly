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
import java.io.PrintWriter;
import java.security.Principal;

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

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

        EjbOverHttpLogger.LOGGER.handlingRequestTo(this.getServletName(), request.getRequestURL());

        request.getSession(true);
        response.setContentType("application/octet-stream");
        final AsyncContext asyncContext = request.startAsync();
        final HttpChannel httpChannel = new HttpChannel(asyncContext);
        final HttpMessageInputStream httpMessageInputStream = new HttpMessageInputStream(asyncContext.getRequest().getInputStream());
        receiver.handleMessage(httpChannel, httpMessageInputStream);

        /*
        PrintWriter writer = response.getWriter();
        Principal principal = null;
        String authType = null;
        String remoteUser = null;

        // Get security principal
        principal = request.getUserPrincipal();
        // Get user name from login principal
        remoteUser = request.getRemoteUser();
        // Get authentication type
        authType = request.getAuthType();

        writer.println(PAGE_HEADER);
        writer.println("<h1>" + "Successfully called Secured EJB " + "</h1>");
        writer.println("<p>" + "Principal  : " + principal + "</p>");
        writer.println("<p>" + "Remote User : " + remoteUser + "</p>");
        writer.println("<p>" + "Authentication Type : " + authType + "</p>");
        writer.println(PAGE_FOOTER);
        writer.close();
        */
    }

    private static String PAGE_HEADER = "<html><head><title>ejb-security</title></head><body>";

    private static String PAGE_FOOTER = "</body></html>";

}
