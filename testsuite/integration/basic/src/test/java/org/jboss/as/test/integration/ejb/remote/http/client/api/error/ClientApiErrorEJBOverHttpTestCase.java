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
package org.jboss.as.test.integration.ejb.remote.http.client.api.error;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.http.client.api.AbstractClientApiEJBOverHttpTestCase;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.logging.Logger;
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
public class ClientApiErrorEJBOverHttpTestCase extends AbstractClientApiEJBOverHttpTestCase {

    private static final Logger logger = Logger.getLogger(ClientApiErrorEJBOverHttpTestCase.class);

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(org.jboss.as.test.integration.ejb.remote.http.EchoRemote.class, org.jboss.as.test.integration.ejb.remote.http.EchoBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);
        return ear;
    }

    @Test
    public void testStateless() throws Exception {
        try {
            final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator<EchoRemote>(EchoRemote.class, APP_NAME,
                    MODULE_NAME, EchoBean.class.getSimpleName(), DISTINCT_NAME);
            final EchoRemote proxy = EJBClient.createProxy(locator);
            proxy.echo("Hello world from a really remote client");
            Assert.fail("loookup and invocation of ejb with wrong class should not succeed");
        } catch (Throwable e) {
            logger.info("got the expected exception", e);
        }
    }

}
