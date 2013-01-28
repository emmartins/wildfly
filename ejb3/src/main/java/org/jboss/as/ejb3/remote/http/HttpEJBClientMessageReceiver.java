package org.jboss.as.ejb3.remote.http;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.as.ejb3.remote.protocol.versionone.HttpVersionOneProtocolChannelReceiver;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 *
 * @author martins
 *
 */
public class HttpEJBClientMessageReceiver implements Channel.Receiver {

    private final ExecutorService executorService;
    private final DeploymentRepository deploymentRepository;
    private final EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository;
    private final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus;
    private final String[] supportedMarshallingStrategies;

    public HttpEJBClientMessageReceiver(ExecutorService executorService, DeploymentRepository deploymentRepository,
            EJBRemoteTransactionsRepository ejbRemoteTransactionsRepository,
            RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus, String[] supportedMarshallingStrategies) {
        this.executorService = executorService;
        this.deploymentRepository = deploymentRepository;
        this.ejbRemoteTransactionsRepository = ejbRemoteTransactionsRepository;
        this.asyncInvocationCancelStatus = asyncInvocationCancelStatus;
        this.supportedMarshallingStrategies = supportedMarshallingStrategies;
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        EjbLogger.EJB3_LOGGER.closingChannel(channel, error);
        try {
            channel.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Override
    public void handleEnd(Channel channel) {
        EjbLogger.EJB3_LOGGER.closingChannelOnChannelEnd(channel);
        try {
            channel.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Override
    public void handleMessage(Channel channel, MessageInputStream messageInputStream) {

        EjbLogger.ROOT_LOGGER.info("HttpEJBClientMessageReceiver:handleMessage()");
        final ChannelAssociation channelAssociation = new HttpChannelAssociation(channel);
        final DataInputStream dataInputStream = new DataInputStream(messageInputStream);
        try {
            final byte version = dataInputStream.readByte();
            final String clientMarshallingStrategy = dataInputStream.readUTF();
            EjbLogger.ROOT_LOGGER.debug("Client with protocol version " + version + " and marshalling strategy "
                    + clientMarshallingStrategy + " trying to communicate on " + channel);
            if (!isSupportedMarshallingStrategy(clientMarshallingStrategy)) {
                EjbLogger.EJB3_LOGGER.unsupportedClientMarshallingStrategy(clientMarshallingStrategy, channel);
                channel.close();
                return;
            }
            switch (version) {
                case 0x01:
                    final MarshallerFactory marshallerFactory = getMarshallerFactory(clientMarshallingStrategy);
                    // enroll VersionOneProtocolChannelReceiver for handling subsequent messages on this channel
                    final HttpVersionOneProtocolChannelReceiver receiver = new HttpVersionOneProtocolChannelReceiver(
                            channelAssociation, deploymentRepository, ejbRemoteTransactionsRepository, marshallerFactory,
                            executorService, asyncInvocationCancelStatus);
                    // ask msg handling, instead of start receiving in remoting one TODO rethink common api, perhaps factory of protocol receiver?
                    // receiver.startReceiving();
                    receiver.handleMessage(channel, messageInputStream);
                    break;

                default:
                    throw EjbMessages.MESSAGES.ejbRemoteServiceCannotHandleClientVersion(version);
            }

        } catch (IOException e) {
            // log it
            EjbLogger.ROOT_LOGGER.exceptionOnChannel(e, channel, messageInputStream);
            IoUtils.safeClose(channel);
        } finally {
            IoUtils.safeClose(messageInputStream);
        }

    }

    private boolean isSupportedMarshallingStrategy(final String strategy) {
        return Arrays.asList(this.supportedMarshallingStrategies).contains(strategy);
    }

    private MarshallerFactory getMarshallerFactory(final String marshallerStrategy) {
        final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory(marshallerStrategy);
        if (marshallerFactory == null) {
            throw EjbMessages.MESSAGES.failedToFindMarshallerFactoryForStrategy(marshallerStrategy);
        }
        return marshallerFactory;
    }
}
