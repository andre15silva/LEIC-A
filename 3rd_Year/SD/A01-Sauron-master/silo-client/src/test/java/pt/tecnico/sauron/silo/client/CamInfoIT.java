package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.CameraDto;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamInfoIT extends SiloIT {

    @Test
    public void camInfoOKTest() throws SiloFrontendException {
        CamInfoRequest request = CamInfoRequest.newBuilder()
                .setCamera(CameraDto.newBuilder().setName("CAMERA1").build())
                .build();
        CamInfoResponse response = frontend.camInfo(request);
        assertEquals("CAMERA1", response.getCamera().getName());
        assertEquals(1, response.getCamera().getCoords().getLatitude());
        assertEquals(-1, response.getCamera().getCoords().getLongitude());
    }

    @Test
    public void camInfoNonExistingTest() {
        CamInfoRequest request = CamInfoRequest.newBuilder()
                .setCamera(CameraDto.newBuilder().setName("CAM").build())
                .build();
        assertEquals(NOT_FOUND.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(request))
                        .getStatus().getCode());
    }

    @Test
    public void camInfoEmptyNameTest() {
        CamInfoRequest request = CamInfoRequest.newBuilder()
                .setCamera(CameraDto.newBuilder().build())
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(request))
                        .getStatus().getCode());
    }

    @Test
    public void camInfoEmptyRequest() {
        CamInfoRequest request = CamInfoRequest.getDefaultInstance();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(request))
                        .getStatus().getCode());
    }

}
