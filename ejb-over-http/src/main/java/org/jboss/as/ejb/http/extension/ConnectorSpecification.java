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
 * POJO describing an EJB over HTTP connection, with an associated builder.
 *
 * Each connector must have a unique virtual-host/context combination, so the identity of
 * each instance of this class is defined by these two attributes.
 *
 * @author sfcoy
 */
final class ConnectorSpecification {
    private final String context;
    private final String virtualHost;
    private final String securityDomain;

    static final class Builder {
        private String context;
        private String virtualHost;
        private String securityDomain;

        Builder() {
            virtualHost = ConnectorModel.DEFAULT_HOST;
        }

        Builder setContext(String context) {
            this.context = context;
            return this;
        }

        Builder setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
            return this;
        }

        Builder setSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        ConnectorSpecification build() {
            return new ConnectorSpecification(context, virtualHost, securityDomain);
        }
    }

    private ConnectorSpecification(String context, String virtualHost, String securityDomain) {
        this.context = context;
        this.virtualHost = virtualHost;
        this.securityDomain = securityDomain;
    }

    public String getContext() {
        return context;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public String getSecurityDomain() {
        return securityDomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorSpecification that = (ConnectorSpecification) o;

        if (!context.equals(that.context)) return false;
        if (!virtualHost.equals(that.virtualHost)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = context.hashCode();
        result = 31 * result + virtualHost.hashCode();
        return result;
    }
}
