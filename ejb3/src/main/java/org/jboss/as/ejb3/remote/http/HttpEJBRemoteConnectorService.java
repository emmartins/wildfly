/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote.http;

import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;

/**
 *
 * @author martins
 *
 */
public class HttpEJBRemoteConnectorService implements Service<HttpEJBRemoteConnectorService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "http-connector");
    public static final String JNDI_NAME = "java:jboss/ejb3/http-connector";

    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();
    private final InjectedValue<EJBRemoteTransactionsRepository> ejbRemoteTransactionsRepositoryInjectedValue = new InjectedValue<EJBRemoteTransactionsRepository>();
    private final InjectedValue<RemoteAsyncInvocationCancelStatusService> remoteAsyncInvocationCancelStatus = new InjectedValue<RemoteAsyncInvocationCancelStatusService>();
    private final String[] supportedMarshallingStrategies;

    public HttpEJBRemoteConnectorService(final String[] supportedMarshallingStrategies) {
        this.supportedMarshallingStrategies = supportedMarshallingStrategies;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // setup message receiver factory
        final ManagedReferenceFactory managedReferenceFactory = new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                return new ValueManagedReference(new ImmediateValue<Object>(
                        HttpEJBRemoteConnectorService.this.getMessageReceiver()));
            }

            @Override
            public String getInstanceClassName() {
                return HttpEJBClientMessageReceiver.class.getName();
            }

            @Override
            public String getJndiViewInstanceValue() {
                return "HttpEJBClientMessageReceiver";
            }
        };
        // add the message receiver to jndi
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(JNDI_NAME);
        final BinderService binderService = new BinderService(bindInfo.getBindName(), this);
        final ServiceBuilder<?> binderBuilder = context
                .getChildTarget()
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addInjection(binderService.getManagedObjectInjector(), managedReferenceFactory)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class,
                        binderService.getNamingStoreInjector());
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private Channel.Receiver getMessageReceiver() {
        return new HttpEJBClientMessageReceiver(executorService.getValue(), deploymentRepositoryInjectedValue.getValue(),
                ejbRemoteTransactionsRepositoryInjectedValue.getValue(), remoteAsyncInvocationCancelStatus.getValue(),
                supportedMarshallingStrategies);
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public HttpEJBRemoteConnectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ExecutorService> getExecutorService() {
        return executorService;
    }

    public Injector<DeploymentRepository> getDeploymentRepositoryInjector() {
        return this.deploymentRepositoryInjectedValue;
    }

    public Injector<EJBRemoteTransactionsRepository> getEJBRemoteTransactionsRepositoryInjector() {
        return this.ejbRemoteTransactionsRepositoryInjectedValue;
    }

    public Injector<RemoteAsyncInvocationCancelStatusService> getAsyncInvocationCancelStatusInjector() {
        return this.remoteAsyncInvocationCancelStatus;
    }
}
