package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.grpc.CtrlPingRequest;
import pt.tecnico.sauron.silo.grpc.CtrlPingResponse;

import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CtrlPingIT extends SiloIT {

    @Test
    public void ctrlPingOKTest() {
        CtrlPingRequest request = CtrlPingRequest
                .newBuilder().setName("client").build();
        CtrlPingResponse response = frontend.ctrlPing(request);
        assertEquals("Hello client!", response.getState());
    }

    @Test
    public void emptyCtrlPingTest() {
        CtrlPingRequest request = CtrlPingRequest
                .newBuilder().setName("").build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.ctrlPing(request))
                        .getStatus().getCode());
    }
}
