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

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * @author sfcoy
 * TODO Finalise message id base
 */
@MessageBundle(projectCode = "EJBHTTP")
public interface EjbOverHttpMessages {

    EjbOverHttpMessages MESSAGES = Messages.getBundle(EjbOverHttpMessages.class);

    @Message(id = 64500, value = "Failed to create EJB-over-HTTP servlet with context %s")
    String createEjbOverHttpServletFailed(String webContext);

    @Message(id = 64501, value = "Failed to start EJB-over-HTTP servlet in virtual host '%s' at context '%s'")
    String startEjbOverHttpServletFailed(String virtualHostName, String webContext);

}
