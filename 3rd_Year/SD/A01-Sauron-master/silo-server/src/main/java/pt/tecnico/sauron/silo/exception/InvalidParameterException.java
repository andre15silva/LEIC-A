package pt.tecnico.sauron.silo.exception;

public class InvalidParameterException extends ServerException {
    // represents invalid parameter events

    public InvalidParameterException(ServerErrorMessage message) {
        super(message);
    }
}
