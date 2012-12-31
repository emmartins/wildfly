package org.jboss.as.ejb3.remote.http;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.remoting3.Channel;

public class HttpEJBRemoteServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Resource(lookup = HttpEJBRemoteConnectorService.JNDI_NAME)
    Channel.Receiver receiver;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final AsyncContext asyncContext = req.startAsync();
        receiver.handleMessage(new HttpChannel(asyncContext), new HttpMessageInputStream(asyncContext.getRequest().getInputStream()));
    }

}
