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
 * Each connector must have a unique virtual-host/context-path combination, so the identity of
 * each instance of this class is defined by these two attributes.
 *
 * @author sfcoy
 * @author martins
 */
final class ConnectorSpecification {

    private final String allowedRoleNames;
    private final String contextPath;
    private final String loginAuthMethod;
    private final String loginRealmName;
    private final String securityDomain;
    private final String virtualHost;

    static final class Builder {

        private String allowedRoleNames;
        private String contextPath;
        private String loginAuthMethod;
        private String loginRealmName;
        private String securityDomain;
        private String virtualHost;

        Builder() {
            virtualHost = ConnectorModel.DEFAULT_HOST;
        }

        Builder setAllowedRoleNames(String allowedRoleNames) {
            this.allowedRoleNames = allowedRoleNames;
            return this;
        }

        Builder setContext(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        Builder setLoginAuthMethod(String loginAuthMethod) {
            this.loginAuthMethod = loginAuthMethod;
            return this;
        }

        Builder setLoginRealmName(String loginRealmName) {
            this.loginRealmName = loginRealmName;
            return this;
        }

        Builder setSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        Builder setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
            return this;
        }

        ConnectorSpecification build() {
            return new ConnectorSpecification(allowedRoleNames, contextPath, loginAuthMethod, loginRealmName, securityDomain, virtualHost);
        }
    }

    private ConnectorSpecification(String allowedRoleNames, String contextPath, String loginAuthMethod, String loginRealmName, String securityDomain, String virtualHost) {
        this.allowedRoleNames = allowedRoleNames;
        this.contextPath = contextPath;
        this.loginAuthMethod = loginAuthMethod;
        this.loginRealmName = loginRealmName;
        this.securityDomain = securityDomain;
        this.virtualHost = virtualHost;
    }

    public String getAllowedRoleNames() {
        return allowedRoleNames;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getLoginAuthMethod() {
        return loginAuthMethod;
    }

    public String getLoginRealmName() {
        return loginRealmName;
    }

    public String getSecurityDomain() {
        return securityDomain;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorSpecification that = (ConnectorSpecification) o;

        if (!contextPath.equals(that.contextPath)) return false;
        if (!virtualHost.equals(that.virtualHost)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contextPath.hashCode();
        result = 31 * result + virtualHost.hashCode();
        return result;
    }
}
