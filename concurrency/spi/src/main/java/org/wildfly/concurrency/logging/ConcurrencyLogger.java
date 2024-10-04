/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.concurrency.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.msc.service.ServiceName;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.RejectedExecutionException;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author <a href="mailto:emartins@redhat.com">Eduardo Martins</a>
 */
@MessageLogger(projectCode = "WFLYCONCUR", length = 4)
public interface ConcurrencyLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ConcurrencyLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ConcurrencyLogger.class, "org.wildfly.concurrency");

    @LogMessage(level = WARN)
    @Message(id = 15, value = "Transaction started in EE Concurrent invocation left open, starting rollback to prevent leak.")
    void rollbackOfTransactionStartedInEEConcurrentInvocation();

    @LogMessage(level = WARN)
    @Message(id = 16, value = "Failed to rollback transaction.")
    void failedToRollbackTransaction(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Failed to suspend transaction.")
    void failedToSuspendTransaction(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 18, value = "System error while checking for transaction leak in EE Concurrent invocation.")
    void systemErrorWhileCheckingForTransactionLeak(@Cause Throwable cause);

    /**
     * Creates an exception indicating the element must provide the attribute.
     *
     * @param element   the element.
     * @param attribute the attribute.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 51, value = "%s elements must provide a %s.")
    IllegalArgumentException elementAttributeMissing(String element, String attribute);

    /**
     * Creates an exception indicating the value for the element is invalid.
     *
     * @param value    the invalid value.
     * @param element  the element.
     * @param location the location of the error.
     *
     * @return {@link XMLStreamException} for the error.
     */
    @Message(id = 66, value = "Invalid value: %s for '%s' element")
    XMLStreamException invalidValue(String value, String element, @Param Location location);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 78, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the service is not started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 82, value = "Service not started")
    IllegalStateException serviceNotStarted();

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 88, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    @Message(id = 102, value = "EE Concurrent Service's value uninitialized.")
    IllegalStateException concurrentServiceValueUninitialized();

    @Message(id = 103, value = "EE Concurrent ContextHandle serialization must be handled by the factory.")
    IOException serializationMustBeHandledByTheFactory();

    @Message(id = 104, value = "The EE Concurrent Context %s already has a factory named %s")
    IllegalArgumentException factoryAlreadyExists(ConcurrentContext concurrentContext, String factoryName);

    @Message(id = 105, value = "EE Concurrent Context %s does not has a factory named %s")
    IOException factoryNotFound(ConcurrentContext concurrentContext, String factoryName);

    @Message(id = 106, value = "EE Concurrent Context %s service not installed.")
    IOException concurrentContextServiceNotInstalled(ServiceName serviceName);

    @Message(id = 107, value = "EE Concurrent Transaction Setup Provider service not installed.")
    IllegalStateException transactionSetupProviderServiceNotInstalled();

    @LogMessage(level = ERROR)
    @Message(id = 110, value = "Failed to run scheduled task: %s")
    void failedToRunTask(Object delegate, @Cause Exception e);

    @Message(id = 111, value = "Cannot run scheduled task %s as container is suspended")
    IllegalStateException cannotRunScheduledTask(Object delegate);

    /**
     * Creates an exception indicating the core-threads must be greater than 0 for the task queue.
     *
     * @param queueLengthValue the queue length value
     *
     * @return an {@link OperationFailedException} for the exception
     */
    @Message(id = 112, value = "The core-threads value must be greater than 0 when the queue-length is %s")
    OperationFailedException invalidCoreThreadsSize(String queueLengthValue);

    /**
     * Creates an exception indicating the max-threads value cannot be less than the core-threads value.
     *
     * @param maxThreads  the size for the max threads
     * @param coreThreads the size for the core threads
     *
     * @return an {@link OperationFailedException} for the exception
     */
    @Message(id = 113, value = "The max-threads value %d cannot be less than the core-threads value %d.")
    OperationFailedException invalidMaxThreads(int maxThreads, int coreThreads);

    @Message(id = 114, value = "Class does not implement all of the provided interfaces")
    IllegalArgumentException classDoesNotImplementAllInterfaces();

    /**
     * Creates an exception indicating the variable, represented by the {@code variable} parameter in the @{code objectType} {@code objectName}, is {@code null}.
     *
     * @param variable the name of the variable.
     * @param objectType the type of the object.
     * @param objectName the name of the object.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 116, value = "%s is null in the %s %s")
    IllegalArgumentException nullVar(String variable, String objectType, String objectName);

    @Message(id = 120, value = "Failed to locate executor service '%s'")
    OperationFailedException executorServiceNotFound(ServiceName serviceName);

    @Message(id = 121, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedExecutorServiceMetric(String attributeName);

    @Message(id = 126, value = "Rejected due to maximum number of requests")
    RejectedExecutionException rejectedDueToMaxRequests();

    /**
     * Logs a warning message indicating a failure when terminating a managed executor's hung task.
     * @param cause     the cause of the error.
     * @param executorName the name of the executor.
     * @param taskName the name of the hung task.
     */
    @LogMessage(level = WARN)
    @Message(id = 128, value = "Failure when terminating %s hung task %s")
    void huntTaskTerminationFailure(@Cause Throwable cause, String executorName, String taskName);

    /**
     * Logs a message indicating a hung task was cancelled.
     * @param executorName the name of the executor.
     * @param taskName the name of the hung task.
     */
    @LogMessage(level = INFO)
    @Message(id = 129, value = "%s hung task %s cancelled")
    void hungTaskCancelled(String executorName, String taskName);

    /**
     * Logs a message indicating a hung task was not cancelled.
     * @param executorName the name of the executor.
     * @param taskName the name of the hung task.
     */
    @LogMessage(level = INFO)
    @Message(id = 130, value = "%s hung task %s not cancelled")
    void hungTaskNotCancelled(String executorName, String taskName);

    @Message(id = 134, value = "Multiple uses of ContextServiceDefinition.ALL_REMAINING")
    IllegalStateException multipleUsesOfAllRemaining();

    @LogMessage(level = WARN)
    @Message(id = 135, value = "Failed to resume transaction.")
    void failedToResumeTransaction(@Cause Throwable cause);

    @Message(id = 136, value = "Failed to run scheduled task: %s")
    RuntimeException failureWhileRunningTask(Object delegate,@Cause Exception e);

    @Message(id = 138, value = "hungTaskTerminationPeriod is not > 0")
    IllegalArgumentException hungTaskTerminationPeriodIsNotBiggerThanZero();
}
