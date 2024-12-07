package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import pt.tecnico.sauron.silo.grpc.CtrlClearRequest;
import pt.tecnico.sauron.silo.grpc.CtrlInitRequest;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloIT extends BaseIT {

    static SiloFrontend frontend;

    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp() throws ZKNamingException, pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException {
        final String zooHost = testProps.getProperty("zoo.host");
        final String zooPort = testProps.getProperty("zoo.port");
        frontend = new SiloFrontend(zooHost, zooPort);
    }

    @AfterAll
    public static void oneTimeTearDown() {
        frontend.close();
    }

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {
        frontend.ctrlInit(CtrlInitRequest.getDefaultInstance());
    }

    @AfterEach
    public void tearDown() {
        frontend.ctrlClear(CtrlClearRequest.getDefaultInstance());
    }

}
