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
package org.jboss.as.test.integration.ejb.remote.http.client.api.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb.http.extension.ConnectorModel;
import org.jboss.as.ejb.http.extension.SubsystemResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicAuthenticationEJBOverHttpSecurityTestCase {

    private static final Logger logger = Logger.getLogger(BasicAuthenticationEJBOverHttpSecurityTestCase.class);

    public static final String APP_NAME = "ejb-remote-client-api-test";
    public static final String MODULE_NAME = "ejb";
    public static final String DISTINCT_NAME = "";
    public static final String CONTEXT_PATH = "/ejb3-remote";

    private final static String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
    private String propertyValue;

    private static final String HOST = "localhost";
    private static final String PORT = "8080";

    public final String NODENAME = "http://" + HOST + ":" + PORT + CONTEXT_PATH + "/";

    private ContextSelector<EJBClientContext> previousSelector;

    @ContainerResource
    private ManagementClient managementClient;

    private PathAddress getPathAddress() {
        final PathAddress subsystemAddress = PathAddress.pathAddress(SubsystemResourceDefinition.SUBSYSTEM_PATH);
        return subsystemAddress.append(ConnectorModel.NAME, ConnectorModel.DEFAULT_HOST + CONTEXT_PATH);
    }

    @Before
    public void before() throws Exception {
        // sys property needed to avoid scan of ejb client properties file
        propertyValue = System.getProperty(propertyName);
        System.setProperty(propertyName, "true");
        // add connector
        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(ConnectorModel.CONTEXT_PATH_ATTR).set(CONTEXT_PATH);
        op.get(ConnectorModel.ALLOWED_ROLE_NAMES_ATTR).set("Users");
        op.get(ConnectorModel.LOGIN_AUTH_METHOD_ATTR).set("BASIC");
        op.get(ConnectorModel.LOGIN_REALM_NAME_ATTR).set("RealmUsersRoles");
        op.get(ConnectorModel.SECURITY_DOMAIN_ATTR).set("other");
        op.get(OP_ADDR).set(getPathAddress().toModelNode());
        ModelNode result = managementClient.getControllerClient().execute(op);
        logger.info("\naddOperation result asString = " + result.asString());
        assertTrue("success".equals(result.get("outcome").asString()));

        // setup client config
        Properties properties = new Properties();
        properties.put("endpoint.name", "ejb-over-http");
        properties.put("remote.connections", "default");
        properties.put("remote.connection.default.transport", "http");
        properties.put("remote.connection.default.host", HOST);
        properties.put("remote.connection.default.port", PORT);
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS", "false");
        properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.CONTEXT_PATH", CONTEXT_PATH);
        //properties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.PREEMPTIVE_BASIC_AUTH", "true");
        properties.put("remote.connection.default.username", "user2");
        properties.put("remote.connection.default.password", "password2");

        // create and activate the selector with the custom config
        final EJBClientConfiguration clientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(clientConfiguration);
        previousSelector = EJBClientContext.setSelector(selector);
    }

    @After
    public void after() throws Exception {
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
        // remove connector
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        op.get(OP_ADDR).set(getPathAddress().toModelNode());
        ModelNode result = managementClient.getControllerClient().execute(op);
        logger.info("\nremoveOperation result asString = " + result.asString());
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        // restore sys property value
        if (propertyValue != null) {
            System.setProperty(propertyName, propertyValue);
        } else {
            System.clearProperty(propertyName);
        }
    }

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(SecuredEchoRemote.class, SecuredEchoBean.class, SecuredCounterRemote.class, SecuredCounterBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);
        return ear;
    }

    @Test
    public void testStateless() throws Exception {
     // get the ejb proxy
        final StatelessEJBLocator<SecuredEchoRemote> locator = new StatelessEJBLocator<SecuredEchoRemote>(SecuredEchoRemote.class, APP_NAME,
                MODULE_NAME, SecuredEchoBean.class.getSimpleName(), DISTINCT_NAME);
        final SecuredEchoRemote proxy = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", proxy);
        // invoke it
        String message = "Hello world from a really remote client 1";
        String echo = proxy.echo(message);
        Assert.assertEquals("Unexpected echo message 1", message, echo);
        message = "Hello world from a really remote client 2";
        echo = proxy.echo(message);
        Assert.assertEquals("Unexpected echo message 2", message, echo);
        message = "Hello world from a really remote client 3";
        echo = proxy.echo(message);
        Assert.assertEquals("Unexpected echo message 3", message, echo);
    }

    @Test
    public void testStatefull() throws Exception {
        // get the ejb proxy
        StatefulEJBLocator<SecuredCounterRemote> locator = EJBClient.createSession(SecuredCounterRemote.class, APP_NAME, MODULE_NAME,
                SecuredCounterBean.class.getSimpleName(), DISTINCT_NAME);
        final SecuredCounterRemote proxy = EJBClient.createProxy(locator);
        Assert.assertNotNull("Received a null proxy", proxy);
        // invoke it
        int counter = proxy.addAndGet(1);
        Assert.assertEquals("Unexpected counter value", 1, counter);
        Thread.sleep(2000);
        counter = proxy.addAndGet(1);
        Assert.assertEquals("Unexpected counter value", 2, counter);
    }

}
