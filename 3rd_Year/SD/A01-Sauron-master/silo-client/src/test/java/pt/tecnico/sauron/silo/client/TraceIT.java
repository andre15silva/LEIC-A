package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

public class TraceIT extends SiloIT {
    @Test
    public void tracePersonOKTest() throws SiloFrontendException {
        TraceRequest request = TraceRequest.newBuilder().setId(Id.newBuilder().setPersonId(1).build()).setType(ObjectType.PERSON).build();
        TraceResponse response = frontend.trace(request);
        assertEquals("CAMERA9", response.getOrderedObs(0).getCamera().getName());
        assertEquals(11, response.getOrderedObsCount());

        long value = -1;
        boolean isSorted = true;
        for (ObservationDto obs : response.getOrderedObsList()) {
            if (value > obs.getTimeStamp().getSeconds()) {
                isSorted = false;
                break;
            } else {
                value = obs.getTimeStamp().getSeconds();
            }
        }
        assertTrue(isSorted);
    }

    @Test
    public void traceCarOKTest() throws SiloFrontendException {
        TraceRequest request = TraceRequest.newBuilder().setId(Id.newBuilder().setCarId("AABB10").build()).setType(ObjectType.CAR).build();
        TraceResponse response = frontend.trace(request);
        assertEquals("CAMERA9", response.getOrderedObs(0).getCamera().getName());
        assertEquals(11, response.getOrderedObsCount());

        long value = -1;
        boolean isSorted = true;
        for (ObservationDto obs : response.getOrderedObsList()) {
            if (value > obs.getTimeStamp().getSeconds()) {
                isSorted = false;
                break;
            } else {
                value = obs.getTimeStamp().getSeconds();
            }
        }
        assertTrue(isSorted);
    }

    @Test
    public void tracePersonEmptyIdTest() {
        TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.PERSON).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trace(request))
                .getStatus().getCode());
    }

    @Test
    public void traceCarEmptyIdTest() {
        TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.CAR).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trace(request))
                .getStatus().getCode());
    }

    @Test
    public void traceEmptyTypeTest() {
        TraceRequest request = TraceRequest.newBuilder().build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trace(request))
                .getStatus().getCode());
    }

    @Test
    public void traceWrongTypeWithIdTest() {
        TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setCarId("AABB10").build()).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trace(request))
                .getStatus().getCode());
    }

    @Test
    public void traceNoSuchCarTest() throws SiloFrontendException {
        TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.CAR).setId(Id.newBuilder().setCarId("LOLOLO").build()).build();
        TraceResponse response = frontend.trace(request);
        assertEquals(0, response.getOrderedObsCount());
    }

    @Test
    public void traceNoSuchPersonTest() throws SiloFrontendException {
        TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setPersonId(-1).build()).build();
        TraceResponse response = frontend.trace(request);
        assertEquals(0, response.getOrderedObsCount());
    }
}
