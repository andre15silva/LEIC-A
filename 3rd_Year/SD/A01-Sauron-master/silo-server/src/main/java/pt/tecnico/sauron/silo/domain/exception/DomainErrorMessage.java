package pt.tecnico.sauron.silo.domain.exception;

public enum DomainErrorMessage {
    NULL_CAMERA_NAME("Camera name cannot be null."),
    NULL_CAMERA_COORDINATES("Camera coordinates cannot be null."),
    CAMERA_NOT_FOUND("No camera with name %s was found."),
    CAMERA_ALREADY_EXISTS("A camera with name %s already exists"),
    NULL_CAMERA_OBSERVATION("Observation camera cannot be null."),
    NULL_OBSERVABLE_TYPE_OBSERVATION("Observation observable type cannot be null."),
    NULL_OBSERVABLE_OBSERVATION("Observation observable cannot be null."),
    NULL_TIMESTAMP_OBSERVATION("Observation timestamp cannot be null."),
    OBSERVATION_CAMERA_NOT_FOUND("Observation camera not found."),
    NULL_TYPE("Type cannot be null."),
    NULL_KEY("Key cannot be null."),
    INVALID_CAR_ID("Car id does not conform to format."),
    INVALID_PERSON_ID("Person id does not conform to format.");

    public final String label;

    DomainErrorMessage(String label) {
        this.label = label;
    }
}
