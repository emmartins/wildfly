/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.service;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.UserTransaction;

/**
 * This {@link UserTransactionWithAccessControlService} checks the permission to access the UserTransaction} before handing out the
 * {@link javax.transaction.UserTransaction} instance from its {@link #getValue()} method.
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
public class UserTransactionWithAccessControlService implements Service<ManagedReferenceFactory> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION_WITH_ACCESS_CONTROL;

    public static ServiceController<ManagedReferenceFactory> addService(final ServiceTarget target, final ServiceVerificationHandler verificationHandler) {
        UserTransactionWithAccessControlService service = new UserTransactionWithAccessControlService();
        return target.addService(SERVICE_NAME, service)
                .addDependency(UserTransactionAccessControlService.SERVICE_NAME, UserTransactionAccessControlService.class, service.getAccessControlService())
                .addDependency(UserTransactionService.SERVICE_NAME, UserTransaction.class, service.getUserTransaction())
                .addListener(verificationHandler)
                .install();
    }

    private final InjectedValue<UserTransactionAccessControlService> accessControlService = new InjectedValue<>();
    private final InjectedValue<UserTransaction> userTransaction = new InjectedValue<>();
    private volatile ManagedReferenceFactory managedReferenceFactory;

    @Override
    public void start(StartContext context) throws StartException {
        managedReferenceFactory = new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public String getJndiViewInstanceValue() {
                return UserTransaction.class.getSimpleName();
            }
            @Override
            public String getInstanceClassName() {
                return UserTransaction.class.getName();
            }
            @Override
            public ManagedReference getReference() {
                accessControlService.getValue().authorizeAccess();
                return new ImmediateManagedReference(userTransaction.getValue());
            }
        };

        ContextNames.bindInfoFor("java:jboss/UserTransaction").bind(context.getChildTarget(), managedReferenceFactory, SERVICE_NAME);
        ContextNames.bindInfoFor("java:comp/UserTransaction").bind(context.getChildTarget(), managedReferenceFactory, SERVICE_NAME);
    }

    @Override
    public void stop(StopContext context) {
        managedReferenceFactory = null;
    }

    @Override
    public ManagedReferenceFactory getValue() throws IllegalStateException {
        if (managedReferenceFactory == null) {
            throw TransactionLogger.ROOT_LOGGER.serviceNotStarted();
        }
        return managedReferenceFactory;
    }

    public InjectedValue<UserTransactionAccessControlService> getAccessControlService() {
        return accessControlService;
    }

    public InjectedValue<UserTransaction> getUserTransaction() {
        return userTransaction;
    }
}
