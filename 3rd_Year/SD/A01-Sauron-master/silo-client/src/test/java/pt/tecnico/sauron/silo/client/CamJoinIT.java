package pt.tecnico.sauron.silo.client;

import com.google.type.LatLng;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.CamJoinResponse;
import pt.tecnico.sauron.silo.grpc.CameraDto;

import static io.grpc.Status.ALREADY_EXISTS;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamJoinIT extends SiloIT {

    @Test
    public void camJoinOKTest() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(50)
                                .setLongitude(-110)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }

    @Test
    public void camJoinAlreadyExistsSameCoordsTest() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("CAMERA0")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(0)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }

    @Test
    public void camJoinAlreadyExistsDifferentCoordsTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("CAMERA0")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(1)
                                .setLongitude(1)
                                .build()))
                .build();
        assertEquals(ALREADY_EXISTS.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }


    @Test
    public void camJoinBadCoordsLimitTest1() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(91)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinBadCoordsLimitTest2() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(-91)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinBadCoordsLimitTest3() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(-181)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinBadCoordsLimitTest4() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(181)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinBadCoordsLimitTest5() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(90)
                                .setLongitude(0)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }

    @Test
    public void camJoinBadCoordsLimitTest6() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(-90)
                                .setLongitude(0)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }

    @Test
    public void camJoinBadCoordsLimitTest7() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(180)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }

    @Test
    public void camJoinBadCoordsLimitTest8() throws SiloFrontendException {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("NEWCAMERA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(-180)
                                .build()))
                .build();
        CamJoinResponse response = frontend.camJoin(request);
        assertEquals(CamJoinResponse.getDefaultInstance(), response);
    }


    @Test
    public void camJoinShortNameTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("AA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinBigNameTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("AAAAAAAAAAAAAAAA")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinNonAlphanumericNameTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("%%%%$$$$")
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinNameNotSetTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setCoords(LatLng.newBuilder()
                                .setLatitude(0)
                                .setLongitude(0)
                                .build()))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinCoordsNotSetTest() {
        CamJoinRequest request = CamJoinRequest.newBuilder()
                .setCamera(CameraDto.newBuilder()
                        .setName("AAAA"))
                .build();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

    @Test
    public void camJoinEmptyRequestTest() {
        CamJoinRequest request = CamJoinRequest.getDefaultInstance();
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(request))
                        .getStatus().getCode());
    }

}
