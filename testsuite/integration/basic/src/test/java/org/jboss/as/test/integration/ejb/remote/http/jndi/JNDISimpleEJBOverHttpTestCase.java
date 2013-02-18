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
package org.jboss.as.test.integration.ejb.remote.http.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.http.AbstractEJBOverHttpTestCase;
import org.jboss.as.test.integration.ejb.remote.http.CounterBean;
import org.jboss.as.test.integration.ejb.remote.http.CounterRemote;
import org.jboss.as.test.integration.ejb.remote.http.EchoBean;
import org.jboss.as.test.integration.ejb.remote.http.EchoRemote;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class JNDISimpleEJBOverHttpTestCase extends AbstractEJBOverHttpTestCase {

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(EchoRemote.class, EchoBean.class, CounterRemote.class, CounterBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);
        return ear;
    }

    protected Context setupClient() throws NamingException {
        // setup client config
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProperties.put("endpoint.name", "http");
        jndiProperties.put("remote.connections", "default");
        jndiProperties.put("remote.connection.default.transport", "http");
        jndiProperties.put("remote.connection.default.host", "localhost");
        jndiProperties.put("remote.connection.default.port", "8080");
        jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.HTTPS", "false");
        jndiProperties.put("remote.connection.default.connect.options.org.jboss.ejb.client.http.HttpOptions.SERVLET_NAME",
                SERVLET_DEPLOYMENT_NAME);
        // without this property the client won't setup the client with the jndi env properties
        jndiProperties.put("org.jboss.ejb.client.scoped.context", "true");
        // create context and lookup bean proxy
        return new InitialContext(jndiProperties);
    }

    @Test
    public void testStateless() throws Exception {
        final Context context = setupClient();
        final EchoRemote proxy = (EchoRemote) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/"
                + EchoBean.class.getSimpleName() + "!" + EchoRemote.class.getName());
        Assert.assertNotNull("Received a null proxy", proxy);
        // invoke the bean
        final String message = "Hello world from a really remote client";
        final String echo = proxy.echo(message);
        Assert.assertEquals("Unexpected echo message", message, echo);
    }

    @Test
    public void testStatefull() throws Exception {
        final Context context = setupClient();
        final CounterRemote proxy = (CounterRemote) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/"
                + CounterBean.class.getSimpleName() + "!" + CounterRemote.class.getName() + "?stateful");
        Assert.assertNotNull("Received a null proxy", proxy);
        // invoke the bean
        int counter = proxy.addAndGet(1);
        Assert.assertEquals("Unexpected counter value", 1, counter);
        counter = proxy.addAndGet(1);
        Assert.assertEquals("Unexpected counter value", 2, counter);
    }

}
