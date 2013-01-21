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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.http.HttpEJBReceiver;
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
 * @author martins
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
    public void tesHttp() throws Exception {
        try {
            // deploy the unmanaged sar
            deployer.deploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
            final HttpEJBReceiver httpEJBReceiver = new HttpEJBReceiver("http://localhost:8080/"+SERVLET_DEPLOYMENT_NAME+"/");
            httpEJBReceiver.registerModule2(APP_NAME, MODULE_NAME, "");
            ContextSelector<EJBClientContext> previousSelector = EJBClientContext.setSelector(new ConfigBasedEJBClientContextSelector(null));
            try {
                EJBClientContext.requireCurrent().registerEJBReceiver(httpEJBReceiver);
                final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
                final EchoRemote proxy = EJBClient.createProxy(locator);
                Assert.assertNotNull("Received a null proxy", proxy);
                final String message = "Hello world from a really remote client";
                final String echo = proxy.echo(message);
                Assert.assertEquals("Unexpected echo message", message, echo);
            } finally {
                // unregister the receiver
                EJBClientContext.requireCurrent().unregisterEJBReceiver(httpEJBReceiver);
                if (previousSelector != null) {
                    EJBClientContext.setSelector(previousSelector);
                }
            }
        } finally {
            // undeploy it
            deployer.undeploy(HttpEJBRemoteServletDeployTestCase.EAR_DEPLOYMENT_NAME);
        }
    }
}
