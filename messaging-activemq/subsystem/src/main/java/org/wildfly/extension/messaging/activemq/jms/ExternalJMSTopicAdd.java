/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX;
import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler adding a topic to the Jakarta Messaging subsystem. The
 * runtime action, will create the {@link JMSTopicService}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSTopicAdd extends AbstractAddStepHandler {

    public static final ExternalJMSTopicAdd INSTANCE = new ExternalJMSTopicAdd();

    private ExternalJMSTopicAdd() {
        super(ExternalJMSTopicDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Do not pass the JNDI bindings to ActiveMQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        final ServiceName jmsTopicServiceName = JMSServices.getJmsTopicBaseServiceName(MessagingServices.getActiveMQServiceName((String) null)).append(name);
        final boolean enabledAMQ1Prefix = ENABLE_AMQ1_PREFIX.resolveModelAttribute(context, model).asBoolean();
        ExternalJMSTopicService jmsTopicService = ExternalJMSTopicService.installService(name, jmsTopicServiceName, serviceTarget, enabledAMQ1Prefix);

        for (String entry : CommonAttributes.DESTINATION_ENTRIES.unwrap(context, model)) {
            ROOT_LOGGER.boundJndiName(entry);
            BinderServiceUtil.installBinderService(serviceTarget, entry, jmsTopicService, jmsTopicServiceName);
        }
    }
}
