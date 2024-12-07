package pt.tecnico.sauron.silo;

import com.google.protobuf.Timestamp;
import com.google.type.LatLng;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Observable;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exception.DomainException;
import pt.tecnico.sauron.silo.domain.exception.NotFoundException;
import pt.tecnico.sauron.silo.domain.exception.PrimaryKeyTakenException;
import pt.tecnico.sauron.silo.exception.InvalidParameterException;
import pt.tecnico.sauron.silo.exception.ServerErrorMessage;
import pt.tecnico.sauron.silo.grpc.*;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.grpc.Status.INTERNAL;
import static io.grpc.Status.INVALID_ARGUMENT;

public final class Silo {

    private static final Logger LOGGER = Logger.getLogger(Silo.class.getName());

    private final SiloRepository siloRepository = SiloRepository.getInstance();

    /**
     * Executes a camJoin request
     *
     * @param request The request to be executed
     * @return The created camera
     * @throws InvalidParameterException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     * @throws NotFoundException
     */
    public Camera camJoin(CamJoinRequest request)
            throws InvalidParameterException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException,
            NotFoundException, PrimaryKeyTakenException {
        try {
            if (!request.hasCamera()) {
                throw new InvalidParameterException(ServerErrorMessage.EMPTY_CAMERA);
            } else if (!request.getCamera().hasCoords()) {
                throw new InvalidParameterException(ServerErrorMessage.CAMERA_COORDS_NOT_VALID);
            }
            String newCamName = request.getCamera().getName();
            if (newCamName.length() < 3 || newCamName.length() > 15 || !newCamName.matches("[a-zA-Z0-9]+")) {
                throw new InvalidParameterException(ServerErrorMessage.CAMERA_NAME_NOT_VALID);
            }

            double lat = request.getCamera().getCoords().getLatitude();
            double lng = request.getCamera().getCoords().getLongitude();
            if (lat > 90 || lat < -90 || lng > 180 || lng < -180) {
                throw new InvalidParameterException(ServerErrorMessage.CAMERA_COORDS_NOT_VALID);
            }
            Camera newCam = new Camera(newCamName, new Location(lat, lng));
            this.siloRepository.addCamera(newCam);
            return newCam;
        } catch (PrimaryKeyTakenException e) {
            // If the camera name is already registered, and the coordinates are the same,
            // no error should be returned.
            Camera cam = this.siloRepository.getCamera(request.getCamera().getName());
            if (!LatLng.newBuilder()
                    .setLatitude(cam.getLocation().getLatitude())
                    .setLongitude(cam.getLocation().getLongitude())
                    .build()
                    .equals(request.getCamera().getCoords())) {
                throw e;
            }
            return cam;
        }
    }

    /**
     * Executes a camInfo request
     *
     * @param request The request to be executed
     * @return The camera requested
     * @throws InvalidParameterException
     * @throws NotFoundException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     */
    public Camera camInfo(CamInfoRequest request) throws InvalidParameterException, NotFoundException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException {
        if (!request.hasCamera()) {
            throw new InvalidParameterException(ServerErrorMessage.EMPTY_ID);
        }

        String camName = request.getCamera().getName();
        return siloRepository.getCamera(camName);
    }

    /**
     * Executes a ctrlPing request
     *
     * @param request          The request to be executed
     * @param responseObserver The stream to which the response is to be sent
     */
    public void ctrlPing(CtrlPingRequest request, StreamObserver<CtrlPingResponse> responseObserver) {
        String input = request.getName();
        LOGGER.info("ping(" + input + ")...");

        if (input == null || input.isBlank()) {
            LOGGER.info("Input cannot be empty!");
            responseObserver.onError(INVALID_ARGUMENT
                    .withDescription("Input cannot be empty!")
                    .asRuntimeException());
        } else {
            CtrlPingResponse response = CtrlPingResponse.newBuilder()
                    .setState("Hello " + input + "!").build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Executes a ctrlClear request
     *
     * @param request          The request to be executed
     * @param responseObserver The stream to which the response is to be sent
     */
    public void ctrlClear(CtrlClearRequest request, StreamObserver<CtrlClearResponse> responseObserver) {
        LOGGER.info("clear...");
        siloRepository.clear();
        responseObserver.onNext(CtrlClearResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /**
     * Executes a ctrlInit request
     *
     * @param request          The request to be executed
     * @param responseObserver The stream to which the response is to be sent
     */
    public void ctrlInit(CtrlInitRequest request, StreamObserver<CtrlInitResponse> responseObserver) {
        LOGGER.info("init...");
        siloRepository.clear();

        try {

            // Add cameras
            for (int i = 0; i < 10; i++) {
                Camera camera = new Camera("CAMERA" + i, new Location(i, -i));
                siloRepository.addCamera(camera);
            }

            // Add observations
            for (int i = 0; i < 500; i++) {
                Observation observation = new Observation(siloRepository.getCamera("CAMERA" + (i % 10)),
                        ObservableType.PERSON, new Person(i + 1), Instant.now());
                siloRepository.addObservation(observation);
            }

            for (int i = 10; i < 100; i++) {
                Observation observation = new Observation(siloRepository.getCamera("CAMERA" + (i % 10)),
                        ObservableType.CAR, new Car("AABB" + i), Instant.now());
                siloRepository.addObservation(observation);
            }

            // Add more Observations for Person 1 and Car "AABB10"
            for (int i = 0; i < 10; ++i) {
                Observation personObservation = new Observation(siloRepository.getCamera("CAMERA" + (i % 10)),
                        ObservableType.PERSON, new Person(1), Instant.now());
                Observation carObservation = new Observation(siloRepository.getCamera("CAMERA" + (i % 10)),
                        ObservableType.CAR, new Car("AABB10"), Instant.now());
                siloRepository.addObservation(personObservation);
                siloRepository.addObservation(carObservation);
            }

            // Add Observations for Car "ZZZZ99" and Person 9999 on CAMERA1
            Observation personObservation = new Observation(siloRepository.getCamera("CAMERA1"),
                    ObservableType.PERSON, new Person(9999), Instant.now());
            Observation carObservation = new Observation(siloRepository.getCamera("CAMERA1"),
                    ObservableType.CAR, new Car("ZZZZ99"), Instant.now());
            siloRepository.addObservation(personObservation);
            siloRepository.addObservation(carObservation);

            // Add another Observation for Car "ZZZZ99" and Person 9999 (the latest ones) on CAMERA2
            personObservation = new Observation(siloRepository.getCamera("CAMERA2"),
                    ObservableType.PERSON, new Person(9999), Instant.now());
            carObservation = new Observation(siloRepository.getCamera("CAMERA2"),
                    ObservableType.CAR, new Car("ZZZZ99"), Instant.now());
            siloRepository.addObservation(personObservation);
            siloRepository.addObservation(carObservation);

            responseObserver.onNext(CtrlInitResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (DomainException e) {
            LOGGER.info(e.getMessage());
            responseObserver.onError(INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    /**
     * Executes a track request
     *
     * @param request The request to be executed
     * @return The observation found or null if not found
     * @throws InvalidParameterException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     */
    public Observation track(TrackRequest request) throws InvalidParameterException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException {
        if (!request.hasId()) {
            throw new InvalidParameterException(ServerErrorMessage.EMPTY_ID);
        }

        ObservableType type = getObservationType(request.getType());
        String id = getObservationId(request.getId(), type);
        List<Observation> observations = siloRepository.getObservationsByTypeKey(type, id, false);

        // Order observations by timestamp and get max value (most recent value)
        return observations.stream().max(Comparator.comparing(Observation::getInstant)).orElse(null);
    }

    /**
     * Executes a trackMatch request
     *
     * @param request The request to be executed
     * @return The list of observations found (possibly empty)
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     * @throws InvalidParameterException
     */
    public List<Observation> trackMatch(TrackMatchRequest request) throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException, InvalidParameterException {
        ObservableType type = getObservationType(request.getType());
        List<Observation> observations = siloRepository.getObservationsByTypeKey(type, request.getPartialId(), true);

        // Get list of observations to each object
        Map<Observable, List<Observation>> observableListMap = observations.stream()
                .collect(Collectors.groupingBy(Observation::getObservable, HashMap::new, Collectors.toList()));

        // Return most recent observation to each object
        return observableListMap.values().stream()
                .map(observationList -> observationList.stream().max(Comparator.comparing(Observation::getInstant)).orElse(null))
                .collect(Collectors.toList());
    }

    /**
     * Executes a trace request
     *
     * @param request The request to be executed
     * @return The list of observations found (possibly empty)
     * @throws InvalidParameterException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     */
    public List<Observation> trace(TraceRequest request) throws InvalidParameterException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException {
        if (!request.hasId()) {
            throw new InvalidParameterException(ServerErrorMessage.EMPTY_ID);
        }

        // Get observations
        ObservableType type = getObservationType(request.getType());
        String id = getObservationId(request.getId(), type);
        List<Observation> observations = siloRepository.getObservationsByTypeKey(type, id, false);

        // Return sorted observations
        return observations.stream()
                .sorted(Comparator.comparing(Observation::getInstant).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Converts an Observation into an ObservationDto.
     *
     * @param observation The observation to be converted
     * @return The equivalent ObservationDto
     * @throws InvalidParameterException
     */
    public static ObservationDto buildObservation(Observation observation)
            throws InvalidParameterException {
        Id id = getObservationId(observation.getObservable().getId(), observation.getObservableType());
        ObjectType type = getObservationType(observation.getObservableType());
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(observation.getInstant().getEpochSecond())
                .build();
        CameraDto camera = buildCamera(observation.getCamera());

        return ObservationDto.newBuilder().setId(id).setTimeStamp(timestamp)
                .setType(type).setCamera(camera).build();
    }

    /**
     * Converts a Camera into a CameraDto.
     *
     * @param camera The camera to be converted
     * @return The equivalent CameraDto
     */
    public static CameraDto buildCamera(Camera camera) {
        return CameraDto.newBuilder()
                .setCoords(LatLng.newBuilder()
                        .setLongitude(camera.getLocation().getLongitude())
                        .setLatitude(camera.getLocation().getLatitude())
                        .build())
                .setName(camera.getId())
                .build();
    }

    /**
     * Executes a report request.
     *
     * @param request The request to be executed
     * @return The list of observations created
     * @throws InvalidParameterException
     * @throws NotFoundException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     */
    public List<Observation> report(ReportRequest request) throws InvalidParameterException, NotFoundException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException {
        List<Observation> observations = new ArrayList<>();
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build();
        Camera camera = siloRepository.getCamera(request.getCameraName());

        for (ObservationDto o : request.getObsList()) {
            if (!o.hasId()) {
                throw new InvalidParameterException(ServerErrorMessage.OBSERVATION_EMPTY_ID);
            }

            if (getObservationType(o.getType()) == ObservableType.CAR) {
                observations.add(new Observation(camera, getObservationType(o.getType()),
                        new Car(o.getId().getCarId()), Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())));
            } else if (getObservationType(o.getType()) == ObservableType.PERSON) {
                observations.add(new Observation(camera, getObservationType(o.getType()),
                        new Person(o.getId().getPersonId()), Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())));
            }
        }

        siloRepository.addObservations(observations);
        return observations;
    }

    /**
     * Adds an observation to the silo.
     *
     * @param observationDto The observationDto of the observation
     * @return The observation created
     * @throws InvalidParameterException
     * @throws NotFoundException
     * @throws pt.tecnico.sauron.silo.domain.exception.InvalidParameterException
     */
    public Observation addObservation(ObservationDto observationDto) throws InvalidParameterException, NotFoundException, pt.tecnico.sauron.silo.domain.exception.InvalidParameterException {
        if (!observationDto.hasId()) {
            throw new InvalidParameterException(ServerErrorMessage.OBSERVATION_EMPTY_ID);
        }

        Timestamp timestamp = observationDto.getTimeStamp();
        Camera camera = siloRepository.getCamera(observationDto.getCamera().getName());

        if (getObservationType(observationDto.getType()) == ObservableType.CAR) {
            Observation observation = new Observation(camera, getObservationType(observationDto.getType()),
                    new Car(observationDto.getId().getCarId()), Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
            siloRepository.addObservation(observation);
            return observation;
        } else {
            Observation observation = new Observation(camera, getObservationType(observationDto.getType()),
                    new Person(observationDto.getId().getPersonId()), Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
            siloRepository.addObservation(observation);
            return observation;
        }
    }

    /**
     * Converts a proto ObjectType into a domain ObservableType.
     *
     * @param type The proto ObjectType to be converted
     * @return The equivalent ObservableType
     * @throws pt.tecnico.sauron.silo.exception.InvalidParameterException
     */
    private ObservableType getObservationType(ObjectType type)
            throws pt.tecnico.sauron.silo.exception.InvalidParameterException {
        switch (type) {
            case PERSON:
                return ObservableType.PERSON;
            case CAR:
                return ObservableType.CAR;
            default:
                throw new InvalidParameterException(ServerErrorMessage.OBSERVABLE_TYPE_NOT_VALID);
        }
    }

    /**
     * Converts a domain ObservableType into a proto ObjectType
     *
     * @param type The domain ObservableType to be converted
     * @return The equivalent ObjectType
     * @throws InvalidParameterException
     */
    private static ObjectType getObservationType(ObservableType type)
            throws InvalidParameterException {
        switch (type) {
            case PERSON:
                return ObjectType.PERSON;
            case CAR:
                return ObjectType.CAR;
            default:
                throw new InvalidParameterException(ServerErrorMessage.OBSERVABLE_TYPE_NOT_VALID);
        }
    }

    /**
     * Convert a proto Id into a domain type id
     *
     * @param id   The proto Id
     * @param type The ObservableType of the object with key Id
     * @return The domain type key
     * @throws InvalidParameterException
     */
    private String getObservationId(Id id, ObservableType type)
            throws InvalidParameterException {
        if (id == null || id.getIdCase().equals(Id.IdCase.ID_NOT_SET)) {
            throw new InvalidParameterException(ServerErrorMessage.EMPTY_ID);
        }

        switch (type) {
            case PERSON:
                if (id.getIdCase().equals(Id.IdCase.PERSONID)) {
                    return String.valueOf(id.getPersonId());
                }
                break;
            case CAR:
                if (id.getIdCase().equals(Id.IdCase.CARID)) {
                    return id.getCarId();
                }
                break;
        }
        throw new InvalidParameterException(ServerErrorMessage.OBSERVABLE_TYPE_NOT_VALID);
    }

    /**
     * Convert a domain type id into a proto type Id
     *
     * @param id   The domain type id
     * @param type The ObservableType of the object with key id
     * @return The ObservableType id
     * @throws InvalidParameterException
     */
    private static Id getObservationId(String id, ObservableType type)
            throws InvalidParameterException {
        switch (type) {
            case PERSON:
                try {
                    long personId = Long.parseLong(id);
                    return Id.newBuilder().setPersonId(personId).build();
                } catch (NumberFormatException e) {
                    throw new InvalidParameterException(ServerErrorMessage.NOT_VALID_PERSON_ID);
                }
            case CAR:
                return Id.newBuilder().setCarId(id).build();
            default:
                throw new InvalidParameterException(ServerErrorMessage.OBSERVABLE_TYPE_NOT_VALID);
        }
    }
}