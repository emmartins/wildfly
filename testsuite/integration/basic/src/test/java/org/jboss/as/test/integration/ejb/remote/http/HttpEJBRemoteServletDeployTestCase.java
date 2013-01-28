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
package org.jboss.as.test.integration.ejb.remote.http;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpEJBRemoteServletDeployTestCase {

    private static final String APP_NAME = "ejb-remote-client-api-test";
    private static final String MODULE_NAME = "ejb";
    private static final String EAR_DEPLOYMENT_NAME = APP_NAME;
    private static final String SERVLET_DEPLOYMENT_NAME = "ejb3-remote";

    @ArquillianResource
    private Deployer deployer;
    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment(name = HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME, managed = false)
    public static Archive<?> createEar() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(EchoRemote.class, EchoBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment
    public static WebArchive createServlet() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SERVLET_DEPLOYMENT_NAME+".war");
        war.setWebXML(HttpEJBRemoteServletDeployTestCase.class.getPackage(), "web.xml");
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.ejb3 \n"), "MANIFEST.MF");
        return war;
    }

    @Test
    public void testClientAPI() throws Exception {
        // there are config files in the classpath, by setting this property the client won't look at it and merge with the test config
        final String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
        final String propertyValue = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName,"true");
            try {
                // deploy the unmanaged sar
                deployer.deploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
                // setup client config
                Properties properties = new Properties();
                properties.put("endpoint.name", "http");
                properties.put("remote.connections","default");
                properties.put("remote.connection.default.transport","http");
                properties.put("remote.connection.default.host","localhost");
                properties.put("remote.connection.default.port","8080");
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS","false");
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.SERVLET_NAME",SERVLET_DEPLOYMENT_NAME);
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTP_CLIENT","jdk");
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.APP_NAME",APP_NAME);
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.MODULE_NAME",MODULE_NAME);
                properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.DISTINCT_NAME","");
                // create and activate the selector with the custom config
                final EJBClientConfiguration clientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
                final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(clientConfiguration);
                ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(selector);
                try {
                    // get the ejb proxy
                    final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator<EchoRemote>(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
                    final EchoRemote proxy = EJBClient.createProxy(locator);
                    Assert.assertNotNull("Received a null proxy", proxy);
                    // invoke it
                    final String message = "Hello world from a really remote client";
                    final String echo = proxy.echo(message);
                    Assert.assertEquals("Unexpected echo message", message, echo);
                } finally {
                    if (previousSelector != null) {
                        EJBClientContext.setSelector(previousSelector);
                    }
                }
            } finally {
                // undeploy it
                deployer.undeploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
            }
        } finally {
            if (propertyValue != null) {
                System.setProperty(propertyName,propertyValue);
            } else {
                System.clearProperty(propertyName);
            }
        }
    }

    @Test
    public void testJNDI() throws Exception {
        // there are config files in the classpath, by setting this property the client won't look at it and merge with the test config
        final String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
        final String propertyValue = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName,"true");
            try {
                // deploy the unmanaged sar
                deployer.deploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
                // setup jndi env
                final Hashtable<String,String> jndiProperties = new Hashtable<String,String>();
                jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
                jndiProperties.put("endpoint.name", "http");
                jndiProperties.put("remote.connections","default");
                jndiProperties.put("remote.connection.default.transport","http");
                jndiProperties.put("remote.connection.default.host","localhost");
                jndiProperties.put("remote.connection.default.port","8080");
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS","false");
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.SERVLET_NAME",SERVLET_DEPLOYMENT_NAME);
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTP_CLIENT","jdk");
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.APP_NAME",APP_NAME);
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.MODULE_NAME",MODULE_NAME);
                jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.DISTINCT_NAME","");
                // without this property the client won't setup the client with the jndi env properties
                jndiProperties.put("org.jboss.ejb.client.scoped.context", "true");
                // create context and lookup bean proxy
                final Context context = new InitialContext(jndiProperties);
                final EchoRemote proxy = (EchoRemote) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + "" + "/" + EchoBean.class.getSimpleName() + "!" + EchoRemote.class.getName());
                Assert.assertNotNull("Received a null proxy", proxy);
                // invoke the bean
                final String message = "Hello world from a really remote client";
                final String echo = proxy.echo(message);
                Assert.assertEquals("Unexpected echo message", message, echo);
            } finally {
                // undeploy it
                deployer.undeploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
            }
        } finally {
            if (propertyValue != null) {
                System.setProperty(propertyName,propertyValue);
            } else {
                System.clearProperty(propertyName);
            }
        }
    }
}
