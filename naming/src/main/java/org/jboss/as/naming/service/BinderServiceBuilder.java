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
package org.jboss.as.naming.service;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Eduardo Martins
 */
public class BinderServiceBuilder {

    private final ContextNames.BindInfo bindInfo;
    private final ServiceTarget serviceTarget;
    private final ServiceVerificationHandler verificationHandler;
    private final ServiceController.Mode initialMode;

    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget) {
        this(bindInfo, serviceTarget, ServiceController.Mode.ACTIVE);
    }

    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        this(bindInfo, serviceTarget, ServiceController.Mode.ACTIVE, verificationHandler);
    }

    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, ServiceController.Mode initialMode) {
        this(bindInfo, serviceTarget, initialMode, null);
    }

    public BinderServiceBuilder(ContextNames.BindInfo bindInfo, ServiceTarget serviceTarget, ServiceController.Mode initialMode, ServiceVerificationHandler verificationHandler) {
        this.bindInfo = bindInfo;
        this.serviceTarget = serviceTarget;
        this.verificationHandler = verificationHandler;
        this.initialMode = initialMode;
    }

    /**
     * Installs a binder service, into the specified service target, where the value's source is an immediate managed reference factory for the specified value.
     * @param value
     * @return
     */
    public ServiceBuilder<ManagedReferenceFactory> addService(Object value) {
        return addService(new ImmediateManagedReferenceFactory(value), value);
    }

    /**
     * Adds a binder service, into the specified service target, where the value's source is the specified managed reference factory.
     * @param valueFactory
     * @param valueSource
     * @return
     */
    public ServiceBuilder<ManagedReferenceFactory> addService(ManagedReferenceFactory valueFactory, Object valueSource) {
        final BinderService binderService = new BinderService(bindInfo.getBindName(), valueSource);
        final ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), valueFactory)
                .setInitialMode(initialMode);
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        return serviceBuilder;
    }

    /**
     * Adds a binder service, into the specified service target, where the value's source is obtained from the service with the specified name.
     * @param valueServiceName
     * @return
     */
    public ServiceBuilder<ManagedReferenceFactory> addService(ServiceName valueServiceName) {
        final BinderService binderService = new BinderService(bindInfo.getBindName(), valueServiceName);
        final ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(valueServiceName, new ManagedReferenceInjector(binderService.getManagedObjectInjector()))
                .setInitialMode(initialMode);
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        return serviceBuilder;
    }

    /**
     * Installs a binder service, into the specified service target, where the value's source is an immediate managed reference factory for the specified value.
     * @param value
     * @return
     */
    public ServiceController<ManagedReferenceFactory> installService(Object value) {
        return addService(value).install();
    }

    /**
     * Installs a binder service, into the specified service target, where the value's source is the specified managed reference factory.
     * @param valueFactory
     * @param valueSource
     * @return
     */
    public ServiceController<ManagedReferenceFactory> installService(ManagedReferenceFactory valueFactory, Object valueSource) {
        return addService(valueFactory, valueSource).install();
    }

    /**
     * Installs a binder service, into the specified service target, where the value's source is obtained from the service with the specified name.
     * @param valueServiceName
     * @return
     */
    public ServiceController<ManagedReferenceFactory> installService(ServiceName valueServiceName) {
        return addService(valueServiceName).install();
    }

    /**
     * Installs a binder service, into the specified service target, where the value's source is the binder service with specified jndi name.
     * @param valueSource
     * @return
     */
    public ServiceController<ManagedReferenceFactory> installService(JndiName valueSource) {
        final ServiceName valueSourceBinderServiceName = ContextNames.bindInfoFor(valueSource).getBinderServiceName();
        final BinderService binderService = new BinderService(bindInfo.getBindName(), valueSourceBinderServiceName);
        final ServiceBuilder serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(valueSourceBinderServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .setInitialMode(initialMode);
        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }
        return serviceBuilder.install();
    }

}
