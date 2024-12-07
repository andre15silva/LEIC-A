package pt.tecnico.sauron.silo.domain;

import java.util.Objects;

public class Camera {
    private String id;
    private Location location;

    public Camera(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public Camera(String id) {
        this.id = id;
        this.location = new Location(0, 0);
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Camera camera = (Camera) o;
        return id.equals(camera.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + "," + location.getLatitude() + "," + location.getLongitude();
    }
}
