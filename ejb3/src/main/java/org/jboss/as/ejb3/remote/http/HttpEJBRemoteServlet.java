package org.jboss.as.ejb3.remote.http;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.as.ejb3.EjbLogger;

public class HttpEJBRemoteServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private HttpEJBClientMessageReceiver receiver;

    private HttpEJBClientMessageReceiver getReceiver() throws ServletException {
        if (receiver == null) {
            try {
                receiver = (HttpEJBClientMessageReceiver) new InitialContext().lookup(HttpEJBRemoteConnectorService.JNDI_NAME);
            } catch (NamingException e) {
                throw new ServletException("failed to retrieve the http message receiver", e);
            }
        }
        return receiver;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp);
    }

    private void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!resp.isCommitted()) {
            final HttpSession httpSession = req.getSession(true);
            EjbLogger.ROOT_LOGGER.info("HttpEJBRemoteServlet:doRequest() "+(httpSession.isNew() ? "new" : "existent")+" session = "+httpSession.getId());
            resp.setContentType("application/octet-stream");
            final AsyncContext asyncContext = req.startAsync();
            final HttpChannel httpChannel = new HttpChannel(asyncContext);
            final HttpMessageInputStream in =  new HttpMessageInputStream(asyncContext.getRequest().getInputStream());
            getReceiver().handleMessage(httpChannel,in);
        }
        else {
            EjbLogger.ROOT_LOGGER.info("HttpEJBRemoteServlet:doRequest() response already committed with status "+resp.getStatus()+". Response = "+resp);
        }
    }

}

