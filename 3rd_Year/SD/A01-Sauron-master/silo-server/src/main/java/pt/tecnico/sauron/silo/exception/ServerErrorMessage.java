package pt.tecnico.sauron.silo.exception;

public enum ServerErrorMessage {
    OBSERVABLE_TYPE_NOT_VALID("Observable type not valid."),
    CAMERA_NAME_NOT_VALID("Camera name is not valid."),
    CAMERA_COORDS_NOT_VALID("Camera coordinates are not valid."),
    EMPTY_CAMERA("Camera cannot be empty."),
    OBSERVATION_EMPTY_ID("Observation id cannot be empty."),
    EMPTY_ID("Id must not be empty."),
    NOT_VALID_PERSON_ID("Id is not a valid person Id."),
    CAMERA_ALREADY_EXISTS("A camera with that id already exists."),
    NOT_VALID_CAR_ID("Id is not a valid car Id."),
    CAMERA_UPDATE_FAILURE("Execution of the camera update failed."),
    OBSERVATION_UPDATE_FAILURE("Execution of the observation update failed.");

    public final String label;

    ServerErrorMessage(String label) {
        this.label = label;
    }
}
