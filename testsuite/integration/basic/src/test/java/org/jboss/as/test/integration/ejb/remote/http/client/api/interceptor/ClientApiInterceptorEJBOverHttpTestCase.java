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
package org.jboss.as.test.integration.ejb.remote.http.client.api.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.client.api.interceptor.RemoteSFSB;
import org.jboss.as.test.integration.ejb.remote.client.api.interceptor.SimpleEJBClientInterceptor;
import org.jboss.as.test.integration.ejb.remote.client.api.interceptor.SimpleSFSB;
import org.jboss.as.test.integration.ejb.remote.http.AbstractEJBOverHttpTestCase;
import org.jboss.as.test.integration.ejb.remote.http.client.api.AbstractClientApiEJBOverHttpTestCase;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
public class ClientApiInterceptorEJBOverHttpTestCase extends AbstractClientApiEJBOverHttpTestCase {

    private static final int CLIENT_INTERCEPTOR_ORDER = 0x99999;

    @Deployment
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(AbstractEJBOverHttpTestCase.class, AbstractClientApiEJBOverHttpTestCase.class);
        jar.addPackage(RemoteSFSB.class.getPackage());
        ear.addAsModule(jar);
        return ear;
    }

   /**
     * @throws Exception
     */
    @Test
    public void testEJBClientInterception() throws Exception {
        // get hold of the EJBClientContext
        final EJBClientContext ejbClientContext = EJBClientContext.requireCurrent();
        // create some data that the client side interceptor will pass along during the EJB invocation
        final Map<String, Object> interceptorData = new HashMap<String, Object>();
        final String keyOne = "foo";
        final Object valueOne = "bar";
        final String keyTwo = "blah";
        final Object valueTwo = new Integer("12");
        interceptorData.put(keyOne, valueOne);
        interceptorData.put(keyTwo, valueTwo);
        final SimpleEJBClientInterceptor clientInterceptor = new SimpleEJBClientInterceptor(interceptorData);
        // register the client side interceptor
        ejbClientContext.registerInterceptor(CLIENT_INTERCEPTOR_ORDER, clientInterceptor);
        // get the ejb proxy
        StatefulEJBLocator<RemoteSFSB> locator = EJBClient.createSession(RemoteSFSB.class, APP_NAME, MODULE_NAME,SimpleSFSB.class.getSimpleName(), DISTINCT_NAME);
        final RemoteSFSB remoteSFSB = EJBClient.createProxy(locator);
        // invoke the bean and ask it for the invocation data that it saw on the server side
        final Map<String, Object> valuesSeenOnServerSide = remoteSFSB.getInvocationData(keyOne, keyTwo);
        // make sure the server side bean was able to get the data which was passed on by the client side interceptor
        Assert.assertNotNull("Server side context data was expected to be non-null", valuesSeenOnServerSide);
        Assert.assertFalse("Server side context data was expected to be non-empty", valuesSeenOnServerSide.isEmpty());
        for (final Map.Entry<String, Object> clientInterceptorDataEntry : interceptorData.entrySet()) {
            final String key = clientInterceptorDataEntry.getKey();
            final Object expectedValue = clientInterceptorDataEntry.getValue();
            Assert.assertEquals("Unexpected value in bean, on server side, via InvocationContext.getContextData() for key " + key, expectedValue, valuesSeenOnServerSide.get(key));
        }
    }

}
