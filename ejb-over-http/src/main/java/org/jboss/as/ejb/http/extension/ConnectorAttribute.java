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
 * @author martins
 */
public enum ConnectorAttribute {

    UNKNOWN(null),

    ALLOWED_ROLE_NAMES(ConnectorModel.ALLOWED_ROLE_NAMES_ATTR),

    CONTEXT_PATH(ConnectorModel.CONTEXT_PATH_ATTR),

    LOGIN_AUTH_METHOD(ConnectorModel.LOGIN_AUTH_METHOD_ATTR),

    LOGIN_REALM_NAME(ConnectorModel.LOGIN_REALM_NAME_ATTR),

    SECURITY_DOMAIN(ConnectorModel.SECURITY_DOMAIN_ATTR),

    VIRTUAL_HOST(ConnectorModel.VIRTUAL_HOST_ATTR);

    private final String localName;

    private ConnectorAttribute(String localName) {
        this.localName = localName;
    }

    String getLocalName() {
        return localName;
    }

    static ConnectorAttribute forLocalName(String localName) {
        if (ALLOWED_ROLE_NAMES.localName.equals(localName))
            return ALLOWED_ROLE_NAMES;
        else if (CONTEXT_PATH.localName.equals(localName))
            return CONTEXT_PATH;
        else if (LOGIN_AUTH_METHOD.localName.equals(localName))
            return LOGIN_AUTH_METHOD;
        else if (LOGIN_REALM_NAME.localName.equals(localName))
            return LOGIN_REALM_NAME;
        else if (SECURITY_DOMAIN.localName.equals(localName))
            return SECURITY_DOMAIN;
        else if (VIRTUAL_HOST.localName.equals(localName))
            return VIRTUAL_HOST;
        else
            return UNKNOWN;
    }

}
