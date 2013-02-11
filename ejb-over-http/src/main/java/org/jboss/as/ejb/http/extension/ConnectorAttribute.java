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

/**
 * EJB over HTTP connector element attributes
 *
 * @author sfcoy
 */
public enum ConnectorAttribute {

    UNKNOWN(null),

    CONTEXT("context"),

    VIRTUAL_HOST("virtual-host"),

    SECURITY_DOMAIN("security-domain");

    private final String localName;

    private ConnectorAttribute(String localName) {
        this.localName = localName;
    }

    String getLocalName() {
        return localName;
    }

    static ConnectorAttribute forLocalName(String localName) {
        if (CONTEXT.localName.equals(localName))
            return CONTEXT;
        else if (VIRTUAL_HOST.localName.equals(localName))
            return VIRTUAL_HOST;
        else if (SECURITY_DOMAIN.localName.equals(localName))
            return SECURITY_DOMAIN;
        else
            return UNKNOWN;
    }

}
