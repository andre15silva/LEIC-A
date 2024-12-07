package pt.tecnico.sauron.silo.domain;

import java.util.Objects;

public abstract class Observable {
    protected String id;

    public Observable() {
    }

    public Observable(String id) {
        this.id = id;
    }


    public synchronized String getId() {
        return id;
    }

    public synchronized void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Observable that = (Observable) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
