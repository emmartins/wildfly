package org.jboss.as.ejb.http.extension;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.*;

/**
 * This is the barebone test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link SubsystemParsingTestCase}
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author sfcoy
 */
public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(EjbOverHttpExtension.SUBSYSTEM_NAME, new EjbOverHttpExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        StringBuilder subsystemXml = new StringBuilder();
        InputStream inputStream = this.getClass().getResourceAsStream("subsystem.xml");
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null)
            subsystemXml.append(inputLine).append('\n');
        return subsystemXml.toString();
    }

}
