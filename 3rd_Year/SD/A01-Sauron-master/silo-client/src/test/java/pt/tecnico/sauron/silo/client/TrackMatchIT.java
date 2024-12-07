package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.ObjectType;
import pt.tecnico.sauron.silo.grpc.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.TrackMatchResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

public class TrackMatchIT extends SiloIT {
    @Test
    public void trackMatchPersonOKTest() throws SiloFrontendException {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.PERSON).setPartialId("10*").build();
        TrackMatchResponse response = frontend.trackMatch(request);

        Set<Long> personIds = response.getUnorderedObsList().stream().map((o) -> o.getId().getPersonId()).collect(Collectors.toSet());
        List<Long> shouldReturn =  Stream.of(100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 10).map(Long::valueOf).collect(Collectors.toList());
        assertEquals(11, personIds.size());
        assertTrue(personIds.containsAll(shouldReturn));
    }

    @Test
    public void trackMatchCarOKTest() throws SiloFrontendException {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR).setPartialId("AABB1*").build();
        TrackMatchResponse response = frontend.trackMatch(request);

        assertEquals(10, response.getUnorderedObsList().size());
        assertTrue(response.getUnorderedObsList().stream().map((o) -> o.getId().getCarId()).collect(Collectors.toSet())
                .containsAll(Stream.iterate(0, (n) -> n+1).limit(10).map(n -> "AABB1" + n).collect(Collectors.toSet())));
    }

    @Test
    public void trackMatchEmptyTypeTest() {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request))
                .getStatus().getCode());
    }

    @Test
    public void trackMatchPersonEmptyPartialIdTest() {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.PERSON).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request))
                .getStatus().getCode());
    }

    @Test
    public void trackMatchCarEmptyPartialIdTest() {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR).build();
        assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request))
                .getStatus().getCode());
    }

    @Test
    public void trackMatchNoSuchCarTest() throws SiloFrontendException {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR).setPartialId("LOLO*").build();
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(0, response.getUnorderedObsCount());
    }

    @Test
    public void trackMatchNoSuchPersonTest() throws SiloFrontendException {
        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.PERSON).setPartialId("129292929*").build();
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(0, response.getUnorderedObsCount());
    }
}
