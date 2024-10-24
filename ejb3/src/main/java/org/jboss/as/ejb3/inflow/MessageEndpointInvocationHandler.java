/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.inflow;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ApplicationServerInternalException;
import jakarta.resource.spi.LocalTransactionException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageEndpointInvocationHandler extends AbstractInvocationHandler implements MessageEndpoint {
    private final MessageEndpointService service;
    private final Object delegate;
    private final XAResource xaRes;
    private final AtomicBoolean released = new AtomicBoolean(false);

    private Transaction currentTx;
    private ClassLoader previousClassLoader;
    private Transaction previousTx;

    MessageEndpointInvocationHandler(final MessageEndpointService service, final Object delegate, final XAResource xaResource) {
        this.service = service;
        this.delegate = delegate;
        this.xaRes = xaResource;
    }

    @Override
    public void afterDelivery() throws ResourceException {
        final TransactionManager tm = getTransactionManager();
        try {
            if (currentTx != null) {
                if (currentTx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                    tm.rollback();
                else
                    tm.commit();
                currentTx = null;
            }
            if (previousTx != null) {
                tm.resume(previousTx);
                previousTx = null;
            }
        } catch (InvalidTransactionException e) {
            throw new LocalTransactionException(e);
        } catch (HeuristicMixedException e) {
            throw new LocalTransactionException(e);
        } catch (SystemException e) {
            throw new LocalTransactionException(e);
        } catch (HeuristicRollbackException e) {
            throw new LocalTransactionException(e);
        } catch (RollbackException e) {
            throw new LocalTransactionException(e);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previousClassLoader);
            previousClassLoader = null;
        }
    }

    @Override
    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        // JCA 1.6 FR 13.5.6
        // The application server must set the thread context class loader to the endpoint
        // application class loader during the beforeDelivery call.
        previousClassLoader = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(getApplicationClassLoader());
        try {
            final TransactionManager tm = getTransactionManager();
            // TODO: in violation of JCA 1.6 FR 13.5.9?
            previousTx = tm.suspend();
            boolean isTransacted = service.isDeliveryTransacted(method);
            if (isTransacted) {
                tm.begin();
                currentTx = tm.getTransaction();
                if (xaRes != null)
                    currentTx.enlistResource(xaRes);
            }
        } catch (Throwable t) {
            throw new ApplicationServerInternalException(t);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previousClassLoader);
        }
    }

    @Override
    protected boolean doEquals(Object obj) {
        if (!(obj instanceof MessageEndpointInvocationHandler))
            return false;

        return delegate.equals(((MessageEndpointInvocationHandler) obj).delegate);
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Are we still usable?
        if (released.get())
            throw EjbLogger.ROOT_LOGGER.messageEndpointAlreadyReleased(this);

        // TODO: check for concurrent invocation

        if (method.getDeclaringClass().equals(MessageEndpoint.class))
            return handle(method, args);

        // TODO: Option A
        try {
            return method.invoke(delegate, args);
        }
        catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    protected final ClassLoader getApplicationClassLoader() {
        return this.service.getClassLoader();
    }

    protected final TransactionManager getTransactionManager() {
        return service.getTransactionManager();
    }

    @Override
    public void release() {
        if (released.getAndSet(true))
            throw new IllegalStateException("Message endpoint " + this + " has already been released");

        // TODO: tidy up outstanding delivery

        service.release(delegate);
    }
}
