package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportIT extends SiloIT {
    private static final int NUM_OBSERVATIONS = 5;

    @Test
    public void reportOKTest() throws SiloFrontendException {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        fillObservations(request);
        ReportResponse reportResponse = frontend.report(request.build());
        assertEquals(reportResponse, ReportResponse.getDefaultInstance());
    }

    @Test
    public void reportNonExistingCameraTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("TESTCAM");
        fillObservations(request);
        assertEquals(NOT_FOUND.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportEmptyCameraNameTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder();
        fillObservations(request);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportEmptyObservationIdTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        request.addObs(0, ObservationDto.getDefaultInstance());
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportInvalidPersonIdTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        fillObservations(request);
        request.addObs(NUM_OBSERVATIONS, ObservationDto.newBuilder().setType(ObjectType.PERSON)
                .setId(Id.newBuilder().setPersonId(-2)));
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportInvalidCarIdTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        fillObservations(request);
        request.addObs(NUM_OBSERVATIONS, ObservationDto.newBuilder().setType(ObjectType.CAR)
                .setId(Id.newBuilder().setCarId("A7AA77")));
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportNoObservationTypeTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        fillObservations(request);
        request.addObs(NUM_OBSERVATIONS, ObservationDto.newBuilder().setId(Id.newBuilder().setPersonId(2)));
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
   }

    @Test
    public void reportWrongObjectTypeTest() {
        ReportRequest.Builder request = ReportRequest.newBuilder().setCameraName("CAMERA1");
        fillObservations(request);
        request.addObs(NUM_OBSERVATIONS, ObservationDto.newBuilder().setType(ObjectType.CAR)
                .setId(Id.newBuilder().setPersonId(2)));
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request.build())).getStatus().getCode());
    }

    @Test
    public void reportEmptyRequestTest() {
        ReportRequest request = ReportRequest.getDefaultInstance();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.report(request)).getStatus().getCode());
    }

    private void fillObservations(ReportRequest.Builder requestBuilder) {
        for (int i = 0; i < NUM_OBSERVATIONS; i++) {
            ObservationDto observation = ObservationDto.newBuilder().setId(Id.newBuilder().setPersonId(i))
                    .setType(ObjectType.PERSON).build();
            requestBuilder.addObs(i, observation);
        }
    }
}
