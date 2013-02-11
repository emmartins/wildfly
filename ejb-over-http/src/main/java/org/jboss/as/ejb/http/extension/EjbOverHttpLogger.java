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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author sfcoy
 * TODO Finalise message id base
 */
@MessageLogger(projectCode = "EJBHTTP")
public interface EjbOverHttpLogger extends BasicLogger {

    EjbOverHttpLogger LOGGER = Logger.getMessageLogger(EjbOverHttpLogger.class, EjbOverHttpLogger.class.getPackage()
            .getName());

    @LogMessage(level = INFO)
    @Message(id = 64550, value = "Starting ejb-over-http service at context root: %s")
    void startingService(String context);

    @LogMessage(level = INFO)
    @Message(id = 64551, value = "Stopping ejb-over-http service at context root: %s")
    void stoppingService(String context);

    @LogMessage(level = DEBUG)
    @Message(id = 64552, value = "Handling EJB request at: %s")
    void handlingRequestTo(StringBuffer requestURL);

    @LogMessage(level = ERROR)
    @Message(id = 64553, value = "Failed to stop web context %s")
    void failedToStopCatalinaStandardContext(String webContext, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 64554, value = "Failed to destroy web context %s")
    void failedToDestroyCatalinaStandardContext(String webContext, @Cause Exception e);

    @LogMessage(level = INFO)
    @Message(id = 64555, value = "ejb-over-http service is not available")
    void ejbOverHttpServiceNotAvailable();

    @LogMessage(level = INFO)
    @Message(id = 64556, value = "Starting servlet deployment for unsecured context [%s] on virtual host [%s]")
    void deployingServlet(String context, String virtualHost);

    @LogMessage(level = INFO)
    @Message(id = 64557, value = "Starting servlet deployment for context [%s] on virtual host [%s], " +
            "secured by security domain [%s]")
    void deployingServlet(String context, String virtualHost, String securityDomain);

}
