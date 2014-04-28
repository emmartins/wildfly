/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Values;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Eduardo Martins
 */
public class ServiceBasedNamingStoreCompositionTestCase {

    private ServiceContainer container;
    private ServiceBasedNamingStore javaCompStore;
    private ServiceBasedNamingStore javaModuleStore;
    private ServiceBasedNamingStore scopedJavaCompStore;
    private ServiceBasedNamingStore scopedJavaModuleStore;

    @Before
    public void setupServiceContainer() throws Exception {
        container = ServiceContainer.Factory.create();
        javaCompStore = new ServiceBasedNamingStore(container, ContextNames.JAVA_COMP_NAME, ContextNames.SHARED_COMP_CONTEXT_SERVICE_NAME);
        javaModuleStore = new ServiceBasedNamingStore(container, ContextNames.JAVA_MODULE_NAME, ContextNames.SHARED_MODULE_CONTEXT_SERVICE_NAME);
        List<ServiceBasedNamingStore> scopedJavaCompOtherNamingStores = new ArrayList<>();
        scopedJavaCompOtherNamingStores.add(javaCompStore);
        scopedJavaCompStore = new ServiceBasedNamingStoreComposition(container, ContextNames.JAVA_COMP_NAME, ContextNames.contextServiceNameOfComponent("app", "module", "comp"), scopedJavaCompOtherNamingStores);
        List<ServiceBasedNamingStore> scopedJavaModuleOtherNamingStores = new ArrayList<>();
        scopedJavaModuleOtherNamingStores.add(javaCompStore);
        scopedJavaModuleOtherNamingStores.add(javaModuleStore);
        scopedJavaModuleStore = new ServiceBasedNamingStoreComposition(container, ContextNames.JAVA_MODULE_NAME, ContextNames.contextServiceNameOfModule("app", "module"), scopedJavaModuleOtherNamingStores);
        bindObject(javaCompStore, ContextNames.bindInfoFor("java:comp/env").getBinderServiceName(), new NamingContext(new CompositeName("env"), javaCompStore, null));
        bindObject(javaCompStore, ContextNames.bindInfoFor("java:comp/env/1").getBinderServiceName(), new Object());
        bindObject(javaCompStore, ContextNames.bindInfoFor("java:comp/comp/1").getBinderServiceName(), new Object());
        bindObject(javaModuleStore, ContextNames.bindInfoFor("java:module/env").getBinderServiceName(), new NamingContext(new CompositeName("env"), javaModuleStore, null));
        bindObject(javaModuleStore, ContextNames.bindInfoFor("java:module/env/2").getBinderServiceName(), new Object());
        bindObject(javaModuleStore, ContextNames.bindInfoFor("java:module/module/2").getBinderServiceName(), new Object());
        bindObject(scopedJavaCompStore, ContextNames.bindInfoFor("app", "module", "comp", "java:comp/env/3").getBinderServiceName(), new Object());
        bindObject(scopedJavaModuleStore, ContextNames.bindInfoFor("app", "module", "java:module/env/4").getBinderServiceName(), new Object());
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
        javaCompStore = null;
        javaModuleStore = null;
        scopedJavaCompStore = null;
        scopedJavaModuleStore = null;
    }

    @Test
    public void testLookup() throws Exception {
        // java:comp entries
        lookup(javaCompStore, "env", NamingContext.class);
        lookup(javaCompStore, "env/1", Object.class);
        lookup(javaCompStore, "comp/1", Object.class);
        // java:module entries
        lookup(javaModuleStore, "env", NamingContext.class);
        lookup(javaModuleStore, "env/2", Object.class);
        lookup(javaModuleStore, "module/2", Object.class);
        // scoped java:comp entries
        lookup(scopedJavaCompStore, "env", NamingContext.class);
        lookup(scopedJavaCompStore, "env/1", Object.class);
        lookup(scopedJavaCompStore, "comp/1", Object.class);
        lookup(scopedJavaCompStore, "env/3", Object.class);
        // scoped java:module entries
        lookup(scopedJavaModuleStore, "env", NamingContext.class);
        lookup(scopedJavaModuleStore, "env/1", Object.class);
        lookup(scopedJavaModuleStore, "comp/1", Object.class);
        lookup(scopedJavaModuleStore, "env/2", Object.class);
        lookup(scopedJavaModuleStore, "module/2", Object.class);
        lookup(scopedJavaModuleStore, "env/4", Object.class);
        // implicit parent contexts
        NamingContext comp = lookup(scopedJavaCompStore, "comp", NamingContext.class);
        NamingContext moduleComp = lookup(scopedJavaModuleStore, "comp", NamingContext.class);
        NamingContext module = lookup(scopedJavaModuleStore, "module", NamingContext.class);
        // nested context lookups
        lookup(comp, "1", Object.class);
        lookup(moduleComp, "1", Object.class);
        lookup(module, "2", Object.class);
    }

    private <T> T lookup(ServiceBasedNamingStore store, String name, Class<? extends T> type) throws NamingException {
        Object obj = store.lookup(new CompositeName(name));
        assertNotNull(obj);
        return type.cast(obj);
    }

    private <T> T lookup(NamingContext namingContext, String name, Class<? extends T> type) throws NamingException {
        Object obj = namingContext.lookup(name);
        assertNotNull(obj);
        return type.cast(obj);
    }

    @Test
    public void testList() throws Exception {
        // java:comp
        List<NameClassPair> javaCompList = javaCompStore.list(new CompositeName(""));
        assertEquals(2, javaCompList.size());
        assertContains(javaCompList, "env", Context.class);
        assertContains(javaCompList, "comp", Context.class);
        List<NameClassPair> javaCompEnvList = javaCompStore.list(new CompositeName("env"));
        assertEquals(1, javaCompEnvList.size());
        assertContains(javaCompEnvList, "1", Object.class);
        List<NameClassPair> javaCompCompList = javaCompStore.list(new CompositeName("comp"));
        assertEquals(1, javaCompCompList.size());
        assertContains(javaCompCompList, "1", Object.class);
        // java:module
        List<NameClassPair> javaModuleList = javaModuleStore.list(new CompositeName(""));
        assertEquals(2, javaModuleList.size());
        assertContains(javaModuleList, "env", Context.class);
        assertContains(javaModuleList, "module", Context.class);
        List<NameClassPair> javaModuleEnvList = javaModuleStore.list(new CompositeName("env"));
        assertEquals(1, javaModuleEnvList.size());
        assertContains(javaModuleEnvList, "2", Object.class);
        List<NameClassPair> javaModuleModuleList = javaModuleStore.list(new CompositeName("module"));
        assertEquals(1, javaModuleModuleList.size());
        assertContains(javaModuleModuleList, "2", Object.class);
        // scoped java:comp
        List<NameClassPair> scopedJavaCompList = scopedJavaCompStore.list(new CompositeName(""));
        assertEquals(2, scopedJavaCompList.size());
        assertContains(scopedJavaCompList, "env", Context.class);
        assertContains(scopedJavaCompList, "comp", Context.class);
        List<NameClassPair> scopedJavaCompEnvList = scopedJavaCompStore.list(new CompositeName("env"));
        assertEquals(2, scopedJavaCompEnvList.size());
        assertContains(scopedJavaCompEnvList, "1", Object.class);
        assertContains(scopedJavaCompEnvList, "3", Object.class);
        List<NameClassPair> scopedJavaCompCompList = scopedJavaCompStore.list(new CompositeName("comp"));
        assertEquals(1, scopedJavaCompCompList.size());
        assertContains(scopedJavaCompCompList, "1", Object.class);
        // scoped java:module
        List<NameClassPair> scopedJavaModuleList = scopedJavaModuleStore.list(new CompositeName(""));
        assertEquals(3, scopedJavaModuleList.size());
        assertContains(scopedJavaModuleList, "env", Context.class);
        assertContains(scopedJavaModuleList, "comp", Context.class);
        assertContains(scopedJavaModuleList, "module", Context.class);
        List<NameClassPair> scopedJavaModuleEnvList = scopedJavaModuleStore.list(new CompositeName("env"));
        assertEquals(3, scopedJavaModuleEnvList.size());
        assertContains(scopedJavaModuleEnvList, "1", Object.class);
        assertContains(scopedJavaModuleEnvList, "2", Object.class);
        assertContains(scopedJavaModuleEnvList, "4", Object.class);
        List<NameClassPair> scopedJavaModuleCompList = scopedJavaModuleStore.list(new CompositeName("comp"));
        assertEquals(1, scopedJavaModuleCompList.size());
        assertContains(scopedJavaModuleCompList, "1", Object.class);
        List<NameClassPair> scopedJavaModuleModuleList = scopedJavaModuleStore.list(new CompositeName("module"));
        assertEquals(1, scopedJavaModuleModuleList.size());
        assertContains(scopedJavaModuleModuleList, "2", Object.class);
    }

    @Test
    public void testListBindings() throws Exception {
        // java:comp
        List<Binding> javaCompList = javaCompStore.listBindings(new CompositeName(""));
        assertEquals(2, javaCompList.size());
        assertContains(javaCompList, "env", Context.class);
        assertContains(javaCompList, "comp", Context.class);
        List<Binding> javaCompEnvList = javaCompStore.listBindings(new CompositeName("env"));
        assertEquals(1, javaCompEnvList.size());
        assertContains(javaCompEnvList, "1", Object.class);
        List<Binding> javaCompCompList = javaCompStore.listBindings(new CompositeName("comp"));
        assertEquals(1, javaCompCompList.size());
        assertContains(javaCompCompList, "1", Object.class);
        // java:module
        List<Binding> javaModuleList = javaModuleStore.listBindings(new CompositeName(""));
        assertEquals(2, javaModuleList.size());
        assertContains(javaModuleList, "env", Context.class);
        assertContains(javaModuleList, "module", Context.class);
        List<Binding> javaModuleEnvList = javaModuleStore.listBindings(new CompositeName("env"));
        assertEquals(1, javaModuleEnvList.size());
        assertContains(javaModuleEnvList, "2", Object.class);
        List<Binding> javaModuleModuleList = javaModuleStore.listBindings(new CompositeName("module"));
        assertEquals(1, javaModuleModuleList.size());
        assertContains(javaModuleModuleList, "2", Object.class);
        // scoped java:comp
        List<Binding> scopedJavaCompList = scopedJavaCompStore.listBindings(new CompositeName(""));
        assertEquals(2, scopedJavaCompList.size());
        assertContains(scopedJavaCompList, "env", Context.class);
        assertContains(scopedJavaCompList, "comp", Context.class);
        List<Binding> scopedJavaCompEnvList = scopedJavaCompStore.listBindings(new CompositeName("env"));
        assertEquals(2, scopedJavaCompEnvList.size());
        assertContains(scopedJavaCompEnvList, "1", Object.class);
        assertContains(scopedJavaCompEnvList, "3", Object.class);
        List<Binding> scopedJavaCompCompList = scopedJavaCompStore.listBindings(new CompositeName("comp"));
        assertEquals(1, scopedJavaCompCompList.size());
        assertContains(scopedJavaCompCompList, "1", Object.class);
        // scoped java:module
        List<Binding> scopedJavaModuleList = scopedJavaModuleStore.listBindings(new CompositeName(""));
        assertEquals(3, scopedJavaModuleList.size());
        assertContains(scopedJavaModuleList, "env", Context.class);
        assertContains(scopedJavaModuleList, "comp", Context.class);
        assertContains(scopedJavaModuleList, "module", Context.class);
        List<Binding> scopedJavaModuleEnvList = scopedJavaModuleStore.listBindings(new CompositeName("env"));
        assertEquals(3, scopedJavaModuleEnvList.size());
        assertContains(scopedJavaModuleEnvList, "1", Object.class);
        assertContains(scopedJavaModuleEnvList, "2", Object.class);
        assertContains(scopedJavaModuleEnvList, "4", Object.class);
        List<Binding> scopedJavaModuleCompList = scopedJavaModuleStore.listBindings(new CompositeName("comp"));
        assertEquals(1, scopedJavaModuleCompList.size());
        assertContains(scopedJavaModuleCompList, "1", Object.class);
        List<Binding> scopedJavaModuleModuleList = scopedJavaModuleStore.listBindings(new CompositeName("module"));
        assertEquals(1, scopedJavaModuleModuleList.size());
        assertContains(scopedJavaModuleModuleList, "2", Object.class);
    }

    private void assertContains(final List<? extends NameClassPair> list, String name, Class<?> type) throws ClassNotFoundException {
        for (NameClassPair value : list) {
            if (value instanceof Binding) {
                assertNotNull(Binding.class.cast(value).getObject());
            }
            if (value.getName().equals(name) && type.isAssignableFrom(Class.forName(value.getClassName()))) {
                return;
            }
        }
        fail("Child [" + name + "] not found in [" + list + "]");
    }

    private void bindObject(final ServiceBasedNamingStore store, final ServiceName serviceName, final Object value) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        container.addService(serviceName, new Service<ManagedReferenceFactory>() {
            public void start(StartContext context) throws StartException {
                store.add(serviceName);
                latch.countDown();
            }

            public void stop(StopContext context) {
            }

            public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
                return new ValueManagedReferenceFactory(Values.immediateValue(value));
            }
        }).install();
        latch.await();
    }
}
