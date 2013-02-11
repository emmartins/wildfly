package org.jboss.as.ejb.http.extension;


import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;


/**
 * Tests all management expects for subsystem, parsing, marshaling, model definition and other
 * Here is an example that allows you a fine grained controller over what is tested and how. So it can give you ideas
 * what can be done and tested.
 * If you have no need for advanced testing of subsystem you look at {@link SubsystemBaseParsingTestCase} that testes same stuff but most of the code
 * is hidden inside of test harness
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemTest {

    public SubsystemParsingTestCase() {
        super(EjbOverHttpExtension.SUBSYSTEM_NAME, new EjbOverHttpExtension());
    }

    private String getSubsystemXml() throws IOException {
        StringBuilder subsystemXml = new StringBuilder();
        InputStream inputStream = this.getClass().getResourceAsStream("subsystem.xml");
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null)
            subsystemXml.append(inputLine).append('\n');
        return subsystemXml.toString();
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        String subsystemXml = getSubsystemXml();
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(4, operations.size());

        //Check that each operation has the correct content
        assertSubsystemAddOperation(operations.get(0));
        assertConnectorAddOperation(operations.get(1), ConnectorModel.DEFAULT_HOST, "ejbs", "other");
        assertConnectorAddOperation(operations.get(2), ConnectorModel.DEFAULT_HOST, "ejbs2");
        assertConnectorAddOperation(operations.get(3), "myhost", "ejbs2");

    }

    private void assertConnectorAddOperation(ModelNode addConnectorOperation, String expectedVirtualHost,
                                             String expectedContext, String ... expectedSecurityDomain) {
        Assert.assertEquals(ADD, addConnectorOperation.get(OP).asString());
        PathAddress pathAddress = PathAddress.pathAddress(addConnectorOperation.get(OP_ADDR));
        Assert.assertEquals(2, pathAddress.size());

        PathElement subSystemPathElement = pathAddress.getElement(0);
        Assert.assertEquals(SUBSYSTEM, subSystemPathElement.getKey());
        Assert.assertEquals(EjbOverHttpExtension.SUBSYSTEM_NAME, subSystemPathElement.getValue());

        PathElement connectorPathElement = pathAddress.getElement(1);
        Assert.assertEquals(ConnectorModel.NAME, connectorPathElement.getKey());
        Assert.assertEquals(expectedVirtualHost + "/" + expectedContext, connectorPathElement.getValue());

        ModelNode virtualHost = addConnectorOperation.get(ConnectorModel.VIRTUAL_HOST_ATTR);
        Assert.assertEquals(expectedVirtualHost, virtualHost.asString());

        ModelNode context = addConnectorOperation.get(ConnectorModel.CONTEXT_ATTR);
        Assert.assertEquals(expectedContext, context.asString());

        if (expectedSecurityDomain.length > 0) {
            ModelNode securityDomain = addConnectorOperation.get(ConnectorModel.SECURITY_DOMAIN_ATTR);
            Assert.assertEquals(expectedSecurityDomain[0], securityDomain.asString());
        }
    }

    private void assertSubsystemAddOperation(ModelNode addSubsystem) {
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress pathAddress = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, pathAddress.size());
        PathElement element = pathAddress.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(EjbOverHttpExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(EjbOverHttpExtension.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(marshalled).build();;
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second
     * controller started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, EjbOverHttpExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();


        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Tests that the subsystem can be removed
     */
    @Test
    public void testSubsystemRemoval() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
        //Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        //TODO Check that any services that were installed were removed here
    }
}
