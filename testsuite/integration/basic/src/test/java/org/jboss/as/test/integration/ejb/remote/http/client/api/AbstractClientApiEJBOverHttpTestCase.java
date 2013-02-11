/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.remote.http.client.api;

import java.util.Properties;

import org.jboss.as.test.integration.ejb.remote.http.AbstractEJBOverHttpTestCase;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Eduardo Martins
 *
 */
public abstract class AbstractClientApiEJBOverHttpTestCase extends AbstractEJBOverHttpTestCase {

    private ContextSelector<EJBClientContext> previousSelector;

    @Before
    public void before() throws Exception {
        super.before();
        // setup client config
        Properties properties = new Properties();
        properties.put("endpoint.name", "ejb-over-http");
        properties.put("remote.connections", "default");
        properties.put("remote.connection.default.transport", "http");
        properties.put("remote.connection.default.host", "localhost");
        properties.put("remote.connection.default.port", "8080");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS", "false");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.SERVLET_NAME",
                SERVLET_DEPLOYMENT_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTP_CLIENT", "jdk");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.APP_NAME", APP_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.MODULE_NAME",
                MODULE_NAME);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.DISTINCT_NAME",
                DISTINCT_NAME);
        // create and activate the selector with the custom config
        final EJBClientConfiguration clientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(clientConfiguration);
        previousSelector = EJBClientContext.setSelector(selector);
    }

    @After
    public void after() throws Exception {
        super.after();
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
    }

}
