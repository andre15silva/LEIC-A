package pt.tecnico.sauron.silo.domain;

import pt.tecnico.sauron.silo.domain.exception.DomainErrorMessage;
import pt.tecnico.sauron.silo.domain.exception.InvalidParameterException;

public class Person extends Observable {

    public Person(long id) throws InvalidParameterException {
        try {
            if (id >= 0) {
                this.setId(String.valueOf(id));
            } else {
                throw new InvalidParameterException(DomainErrorMessage.INVALID_PERSON_ID);
            }
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(DomainErrorMessage.INVALID_PERSON_ID);
        }
    }

    @Override
    public String toString() {
        return "person," + id;
    }
}