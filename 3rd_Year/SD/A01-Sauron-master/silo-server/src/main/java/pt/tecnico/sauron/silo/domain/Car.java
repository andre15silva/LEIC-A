package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exception.DomainErrorMessage;
import pt.tecnico.sauron.silo.domain.exception.InvalidParameterException;

public class Car extends Observable {

    public Car(String id) throws InvalidParameterException {
        if (id.matches("^[A-Z]{4}[0-9]{2}$|^[A-Z]{2}[0-9]{2}[A-Z]{2}$|^[0-9]{2}[A-Z]{4}$|^[0-9]{4}[A-Z]{2}$|^[0-9]{2}[A-Z]{2}[0-9]{2}$|^[A-Z]{2}[0-9]{4}$")) {
            this.setId(id);
        } else {
            throw new InvalidParameterException(DomainErrorMessage.INVALID_CAR_ID);
        }
    }

    @Override
    public String toString() {
        return "car," + id;
    }
}