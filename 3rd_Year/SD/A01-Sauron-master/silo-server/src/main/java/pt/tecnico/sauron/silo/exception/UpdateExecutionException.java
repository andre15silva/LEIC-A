package pt.tecnico.sauron.silo.exception;

public class UpdateExecutionException extends ServerException {

    public UpdateExecutionException(ServerErrorMessage message) {
        super(message);
    }
}
