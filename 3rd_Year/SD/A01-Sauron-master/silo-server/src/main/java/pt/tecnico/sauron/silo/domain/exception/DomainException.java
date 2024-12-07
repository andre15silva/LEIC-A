package pt.tecnico.sauron.silo.domain.exception;

public abstract class DomainException extends Exception {
    // super class

    public DomainException(DomainErrorMessage message) {
        super(message.label);
    }

    public DomainException(DomainErrorMessage message, String arg) {
        super(String.format(message.label, arg));
    }
}