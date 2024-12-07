package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TrackIT extends SiloIT {

    @Test
    public void trackPersonOKTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setPersonId(1).build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObjectType.PERSON, response.getMostRecentObs().getType());
        assertEquals(Id.newBuilder().setPersonId(1).build(), response.getMostRecentObs().getId());
        assertEquals("CAMERA9", response.getMostRecentObs().getCamera().getName());
    }

    @Test
    public void trackCarOKTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.CAR).setId(Id.newBuilder().setCarId("AABB10").build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObjectType.CAR, response.getMostRecentObs().getType());
        assertEquals(Id.newBuilder().setCarId("AABB10").build(), response.getMostRecentObs().getId());
        assertEquals("CAMERA9", response.getMostRecentObs().getCamera().getName());
    }

    @Test
    public void trackPersonEmptyIdTest() {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.track(request))
                .getStatus().getCode());
    }

    @Test
    public void trackCarEmptyIdTest() {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.CAR).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.track(request))
                .getStatus().getCode());
    }

    @Test
    public void trackEmptyType() {
        TrackRequest request = TrackRequest.newBuilder().build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.track(request))
                .getStatus().getCode());
    }

    @Test
    public void trackWrongTypeWithIdTest() {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setCarId("AABB10").build()).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.track(request))
                .getStatus().getCode());
    }

    @Test
    public void trackNoSuchCarTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.CAR).setId(Id.newBuilder().setCarId("LOLOLO").build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObservationDto.getDefaultInstance(), response.getMostRecentObs());
    }

    @Test
    public void trackNoSuchPersonTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setPersonId(-1).build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObservationDto.getDefaultInstance(), response.getMostRecentObs());
    }

    @Test
    public void trackPersonAssertLatestTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON).setId(Id.newBuilder().setPersonId(9999).build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObjectType.PERSON, response.getMostRecentObs().getType());
        assertEquals(Id.newBuilder().setPersonId(9999).build(), response.getMostRecentObs().getId());
        assertEquals("CAMERA2", response.getMostRecentObs().getCamera().getName());
    }

    @Test
    public void trackCarAssertLatestTest() throws SiloFrontendException {
        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.CAR).setId(Id.newBuilder().setCarId("ZZZZ99").build()).build();
        TrackResponse response = frontend.track(request);
        assertEquals(ObjectType.CAR, response.getMostRecentObs().getType());
        assertEquals(Id.newBuilder().setCarId("ZZZZ99").build(), response.getMostRecentObs().getId());
        assertEquals("CAMERA2", response.getMostRecentObs().getCamera().getName());
    }
}

