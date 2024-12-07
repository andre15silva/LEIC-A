package pt.tecnico.sauron.silo.exception;

public abstract class ServerException extends Exception {
    // super class

    public ServerException(ServerErrorMessage message) {
        super(message.label);
    }
}