package pt.tecnico.sauron.silo.domain;

import java.time.Instant;
import java.util.Objects;

public class Observation {
    private Camera camera;
    private ObservableType observableType;
    private Observable observable;
    private Instant instant;

    public Observation(Camera camera, ObservableType observableType, Observable observable, Instant instant) {
        this.camera = camera;
        this.observableType = observableType;
        this.observable = observable;
        this.instant = instant;
    }

    public synchronized Camera getCamera() {
        return camera;
    }

    public synchronized ObservableType getObservableType() {
        return observableType;
    }

    public synchronized Instant getInstant() {
        return instant;
    }

    public synchronized Observable getObservable() {
        return observable;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public String toString() {
        return observable.toString() + "," + instant.toString() + "," + camera.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Observation that = (Observation) o;
        return Objects.equals(camera, that.camera) &&
                observableType == that.observableType &&
                Objects.equals(observable, that.observable) &&
                Objects.equals(instant, that.instant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, observableType, observable, instant);
    }
}

