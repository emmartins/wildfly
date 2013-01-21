package org.jboss.as.ejb3.remote.http;

import java.io.IOException;

import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageOutputStream;

public class HttpChannelAssociation extends ChannelAssociation {

    public HttpChannelAssociation(Channel channel) {
        super(channel);
    }

    @Override
    public MessageOutputStream acquireChannelMessageOutputStream() throws Exception {
        return getChannel().writeMessage();
    }

    @Override
    public void releaseChannelMessageOutputStream(MessageOutputStream messageOutputStream) throws IOException {
        try {
            messageOutputStream.close();
        }
        finally {
            getChannel().close();
        }
    }
}
