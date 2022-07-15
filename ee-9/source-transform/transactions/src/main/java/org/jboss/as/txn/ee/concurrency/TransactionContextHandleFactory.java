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
package org.jboss.as.txn.ee.concurrency;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.as.ee.concurrent.handle.EE10ContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.as.server.deployment.DelegatingSupplier;
import org.jboss.as.txn.logging.TransactionLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * A context handle factory which is responsible for preventing transaction leaks.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionContextHandleFactory implements EE10ContextHandleFactory {

    public static final String NAME = ContextServiceDefinition.TRANSACTION;

    private final DelegatingSupplier<TransactionManager> transactionManager = new DelegatingSupplier<>();

    @Override
    public String getContextType() {
        return NAME;
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        System.out.println("TransactionContextHandleFactory#clearedContext()");
        if (contextObjectProperties != null && ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(contextObjectProperties.get(ManagedTask.TRANSACTION))) {
            // override to unchanged
            System.out.println("TransactionContextHandleFactory#clearedContext() overrided to unchanged");
            return null;
        }
        return new ClearedSetupContextHandle(transactionManager.get());
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        // FIXME not required by spec, should we support it?!?
        return unchangedContext(contextService, contextObjectProperties);
    }

    @Override
    public SetupContextHandle unchangedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        System.out.println("TransactionContextHandleFactory#unchangedContext()");
        if (contextObjectProperties != null && ManagedTask.SUSPEND.equals(contextObjectProperties.get(ManagedTask.TRANSACTION))) {
            // override to cleared
            System.out.println("TransactionContextHandleFactory#unchangedContext() overrided to clear");
            return new ClearedSetupContextHandle(transactionManager.get());
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        // TODO confirm this should be first (same way as tx setup provider is run before context handle save)
        return 10;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return new ClearedSetupContextHandle(transactionManager.get());
    }

    public DelegatingSupplier<TransactionManager> getTransactionManagerSupplier() {
        return transactionManager;
    }

    private static class ClearedSetupContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 5751959084132309889L;
        private final TransactionManager transactionManager;

        private ClearedSetupContextHandle(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            System.out.println("ClearedSetupContextHandle#setup()");
            Transaction transactionOnSetup = null;
            if(transactionManager != null) {
                try {
                    transactionOnSetup = transactionManager.suspend();
                    System.out.println("ClearedSetupContextHandle#setup() tx suspended");
                } catch (SystemException e) {
                    e.printStackTrace();
                    throw new IllegalStateException(e);
                }
            }
            return new ClearedResetContextHandle(transactionManager, transactionOnSetup);
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }

    private static class ClearedResetContextHandle implements ResetContextHandle {

        private static final long serialVersionUID = 6601621971677254974L;
        private final TransactionManager transactionManager;
        private final Transaction transactionOnSetup;

        private ClearedResetContextHandle(TransactionManager transactionManager, Transaction transactionOnSetup) {
            this.transactionManager = transactionManager;
            this.transactionOnSetup = transactionOnSetup;
        }

        @Override
        public void reset() {
            System.out.println("ClearedSetupContextHandle#reset()");
            try {
                transactionManager.resume(transactionOnSetup);
                System.out.println("ClearedSetupContextHandle#reset() resumed tx");
            } catch (Throwable e) {
                TransactionLogger.ROOT_LOGGER.warn("failed to resume transaction",e);
            }
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        // serialization

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw TransactionLogger.ROOT_LOGGER.serializationMustBeHandledByTheFactory();
        }
    }
}
