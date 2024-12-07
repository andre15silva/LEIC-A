package pt.tecnico.sauron.silo.domain.exception;

public class PrimaryKeyTakenException extends DomainException {
    // represents PK constrain exception

    public PrimaryKeyTakenException(DomainErrorMessage message, String id) {
        super(message, id);
    }
}
