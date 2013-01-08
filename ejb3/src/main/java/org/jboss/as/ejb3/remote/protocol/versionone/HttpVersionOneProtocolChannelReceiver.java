/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 *
 * @author martins
 *
 */
public class HttpVersionOneProtocolChannelReceiver implements Channel.Receiver {

    private static final byte HEADER_SESSION_OPEN_REQUEST = 0x01;
    private static final byte HEADER_INVOCATION_REQUEST = 0x03;
    private static final byte HEADER_INVOCATION_CANCELLATION_REQUEST = 0x04;
    private static final byte HEADER_TX_COMMIT_REQUEST = 0x0F;
    private static final byte HEADER_TX_ROLLBACK_REQUEST = 0x10;
    private static final byte HEADER_TX_PREPARE_REQUEST = 0x11;
    private static final byte HEADER_TX_FORGET_REQUEST = 0x12;
    private static final byte HEADER_TX_BEFORE_COMPLETION_REQUEST = 0x13;

    private final ChannelAssociation channelAssociation;
    private final DeploymentRepository deploymentRepository;
    private final EJBRemoteTransactionsRepository transactionsRepository;
    private final MarshallerFactory marshallerFactory;
    private final ExecutorService executorService;
    private final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus;

    public HttpVersionOneProtocolChannelReceiver(final ChannelAssociation channelAssociation,
            final DeploymentRepository deploymentRepository, final EJBRemoteTransactionsRepository transactionsRepository,
            final MarshallerFactory marshallerFactory, final ExecutorService executorService,
            final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatusService) {
        this.marshallerFactory = marshallerFactory;
        this.channelAssociation = channelAssociation;
        this.executorService = executorService;
        this.deploymentRepository = deploymentRepository;
        this.transactionsRepository = transactionsRepository;
        this.remoteAsyncInvocationCancelStatus = asyncInvocationCancelStatusService;
    }

    public void startReceiving() {
        // do nothing
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        try {
            channel.close();
        } catch (IOException e) {
            throw EjbMessages.MESSAGES.couldNotCloseChannel(e);
        }
    }

    @Override
    public void handleEnd(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
        try {
            // read the first byte to see what type of a message it is
            final int header = messageInputStream.read();
            if (EjbLogger.ROOT_LOGGER.isTraceEnabled()) {
                EjbLogger.ROOT_LOGGER.trace("Got message with header 0x" + Integer.toHexString(header) + " on channel "
                        + channel);
            }
            MessageHandler messageHandler = null;
            switch (header) {
                case HEADER_INVOCATION_REQUEST:
                    messageHandler = new MethodInvocationMessageHandler(this.deploymentRepository, this.marshallerFactory,
                            this.executorService, this.remoteAsyncInvocationCancelStatus);
                    break;
                case HEADER_INVOCATION_CANCELLATION_REQUEST:
                    messageHandler = new InvocationCancellationMessageHandler(this.remoteAsyncInvocationCancelStatus);
                    break;
                case HEADER_SESSION_OPEN_REQUEST:
                    messageHandler = new SessionOpenRequestHandler(this.deploymentRepository, this.marshallerFactory,
                            this.executorService);
                    break;
                case HEADER_TX_COMMIT_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory,
                            this.executorService, TransactionRequestHandler.TransactionRequestType.COMMIT);
                    break;
                case HEADER_TX_ROLLBACK_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory,
                            this.executorService, TransactionRequestHandler.TransactionRequestType.ROLLBACK);
                    break;
                case HEADER_TX_FORGET_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory,
                            this.executorService, TransactionRequestHandler.TransactionRequestType.FORGET);
                    break;
                case HEADER_TX_PREPARE_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory,
                            this.executorService, TransactionRequestHandler.TransactionRequestType.PREPARE);
                    break;
                case HEADER_TX_BEFORE_COMPLETION_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory,
                            this.executorService, TransactionRequestHandler.TransactionRequestType.BEFORE_COMPLETION);
                    break;
                default:
                    EjbLogger.ROOT_LOGGER.unsupportedMessageHeader(Integer.toHexString(header), channel);
                    return;
            }
            // process the message
            messageHandler.processMessage(channelAssociation, messageInputStream);
            // enroll for next message (whenever it's available)
            channel.receiveMessage(this);

        } catch (IOException e) {
            // log it
            EjbLogger.ROOT_LOGGER.exceptionOnChannel(e, channel, messageInputStream);
            // no more messages can be sent or received on this channel
            IoUtils.safeClose(channel);
        } finally {
            IoUtils.safeClose(messageInputStream);
        }
    }

}
