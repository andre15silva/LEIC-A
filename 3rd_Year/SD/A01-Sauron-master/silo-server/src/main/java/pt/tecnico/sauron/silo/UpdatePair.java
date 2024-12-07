package pt.tecnico.sauron.silo;

import java.util.List;
import java.util.Objects;

public class UpdatePair {

    private Integer replicaId;

    private List<Long> prevVectorTimestamp;

    public UpdatePair(Integer replicaId, List<Long> prevVectorTimestamp) {
        this.replicaId = replicaId;
        this.prevVectorTimestamp = prevVectorTimestamp;
    }

    public Integer getReplicaId() {
        return replicaId;
    }

    public List<Long> getPrevVectorTimestamp() {
        return prevVectorTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdatePair that = (UpdatePair) o;
        return Objects.equals(replicaId, that.replicaId) &&
                Objects.equals(prevVectorTimestamp, that.prevVectorTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicaId, prevVectorTimestamp);
    }

    @Override
    public String toString() {
        return "UpdatePair{" +
                "replicaId=" + replicaId +
                ", prevVectorTimestamp=" + prevVectorTimestamp +
                '}';
    }
}
