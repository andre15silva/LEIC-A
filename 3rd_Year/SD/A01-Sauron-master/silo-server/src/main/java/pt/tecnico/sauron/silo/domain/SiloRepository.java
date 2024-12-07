package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exception.DomainErrorMessage;
import pt.tecnico.sauron.silo.domain.exception.InvalidParameterException;
import pt.tecnico.sauron.silo.domain.exception.NotFoundException;
import pt.tecnico.sauron.silo.domain.exception.PrimaryKeyTakenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiloRepository {
    private static final SiloRepository instance = new SiloRepository();
    private Map<String, Camera> cameras = new HashMap<>();
    private List<Observation> observations = new ArrayList<>();

    private SiloRepository() {
    }

    public static SiloRepository getInstance() {
        return instance;
    }

    public synchronized void addCamera(Camera camera) throws PrimaryKeyTakenException, InvalidParameterException {
        if (camera.getId() == null || camera.getId().isBlank()) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_CAMERA_NAME);
        } else if (camera.getLocation() == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_CAMERA_COORDINATES);
        } else if (cameras.get(camera.getId()) != null) {
            throw new PrimaryKeyTakenException(DomainErrorMessage.CAMERA_ALREADY_EXISTS, camera.getId());
        }

        cameras.put(camera.getId(), camera);
    }

    private void checkObservation(Observation observation) throws NotFoundException, InvalidParameterException {
        if (observation.getCamera() == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_CAMERA_OBSERVATION);
        } else if (observation.getObservableType() == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_OBSERVABLE_TYPE_OBSERVATION);
        } else if (observation.getObservable() == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_OBSERVABLE_OBSERVATION);
        } else if (observation.getInstant() == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_TIMESTAMP_OBSERVATION);
        } else if (cameras.get(observation.getCamera().getId()) == null) {
            throw new NotFoundException(DomainErrorMessage.OBSERVATION_CAMERA_NOT_FOUND);
        }
    }

    public void addObservations(List<Observation> observations) throws NotFoundException, InvalidParameterException {
        for (Observation o : observations) {
            addObservation(o);
        }
    }

    public synchronized void addObservation(Observation observation) throws NotFoundException, InvalidParameterException {
        checkObservation(observation);
        observations.add(observation);
    }

    public synchronized Camera getCamera(String key) throws InvalidParameterException, NotFoundException {
        if (key == null || key.isBlank()) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_CAMERA_NAME);
        } else if (cameras.get(key) == null) {
            throw new NotFoundException(DomainErrorMessage.CAMERA_NOT_FOUND, key);
        }

        return cameras.get(key);
    }

    public synchronized List<Observation> getObservationsByTypeKey(ObservableType type, String key, boolean match) throws InvalidParameterException {
        if (type == null) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_TYPE);
        } else if (key == null || key.isBlank()) {
            throw new InvalidParameterException(DomainErrorMessage.NULL_KEY);
        }

        if (match) {
            String pattern = key.replace("*", ".*");
            return observations.stream()
                    .filter(observation -> observation.getObservableType().equals(type) &&
                            observation.getObservable().getId().matches(pattern))
                    .collect(Collectors.toList());
        } else {
            return observations.stream()
                    .filter(observation -> observation.getObservableType().equals(type) &&
                            observation.getObservable().getId().equals(key))
                    .collect(Collectors.toList());
        }
    }

    public synchronized void clear() {
        cameras.clear();
        observations.clear();
    }
}