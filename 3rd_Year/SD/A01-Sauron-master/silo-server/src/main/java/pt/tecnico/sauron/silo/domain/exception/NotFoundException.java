package pt.tecnico.sauron.silo.domain.exception;

public class NotFoundException extends DomainException {
    // represents not found events

    public NotFoundException(DomainErrorMessage message) {
        super(message);
    }

    public NotFoundException(DomainErrorMessage message, String id) {
        super(message, id);
    }
}
