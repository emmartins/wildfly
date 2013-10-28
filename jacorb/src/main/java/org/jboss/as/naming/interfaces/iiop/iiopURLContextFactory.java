package org.jboss.as.naming.interfaces.iiop;

import org.jboss.as.jacorb.naming.jndi.JBossCNCtxFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * @author Eduardo Martins
 */
public class iiopURLContextFactory implements ObjectFactory {

    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        return JBossCNCtxFactory.INSTANCE.getObjectInstance(obj, name, nameCtx, environment);
    }
}
