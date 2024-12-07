package pt.tecnico.sauron.silo.domain.exception;

public class InvalidParameterException extends DomainException {
    // represents invalid parameter events

    public InvalidParameterException(DomainErrorMessage message) {
        super(message);
    }
}
