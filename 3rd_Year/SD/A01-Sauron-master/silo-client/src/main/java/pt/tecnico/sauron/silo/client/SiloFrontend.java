package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.util.*;

public class SiloFrontend implements AutoCloseable {
    private ReplicaManagerGrpc.ReplicaManagerBlockingStub stub;
    private ManagedChannel channel;

    private ArrayList<Long> vectorTimestamp = new ArrayList<>();
    private ResponseCache responseCache = new ResponseCache();
    private ZKNaming zkNaming;

    /**
     * Creates a SiloFrontend that will contact the nameserver on zooHost:zooPort, which will initially contact /grpc/sauron/silo/replicaId.
     * The frontend's cache will have size zero, meaning it won't be used.
     *
     * @param zooHost   The nameserver's host
     * @param zooPort   The nameserver's port
     * @param replicaId The id of the replica to be contacted
     * @throws ZKNamingException
     * @throws SiloFrontendException
     */
    public SiloFrontend(String zooHost, String zooPort, String replicaId) throws ZKNamingException, SiloFrontendException {
        zkNaming = new ZKNaming(zooHost, zooPort);
        try {
            // lookup
            ZKRecord record = zkNaming.lookup(String.format("/grpc/sauron/silo/%d", Integer.parseInt(replicaId)));
            String target = record.getURI();

            // setup vectorTimestamp
            this.vectorTimestamp.addAll(Collections.nCopies(Integer.parseInt(replicaId), 0L));

            this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            this.stub = ReplicaManagerGrpc.newBlockingStub(channel);
        } catch (ZKNamingException e) {
            connectToRandomReplica();
        }
    }

    /**
     * Creates a SiloFrontend that will contact the nameserver on zooHost:zooPort, which will contact any replica.
     * The frontend's cache will have size zero, meaning it won't be used.
     *
     * @param zooHost The nameserver's host
     * @param zooPort The nameserver's port
     * @throws ZKNamingException
     * @throws SiloFrontendException
     */
    public SiloFrontend(String zooHost, String zooPort) throws ZKNamingException, SiloFrontendException {
        zkNaming = new ZKNaming(zooHost, zooPort);
        connectToRandomReplica();
    }

    /**
     * Creates a SiloFrontend that will contact the nameserver on zooHost:zooPort, which will contact any replica.
     * Initializes the frontend's cache with size cacheSize.
     *
     * @param zooHost   The nameserver's host
     * @param zooPort   The nameserver's port
     * @param cacheSize Initial cache size
     * @throws ZKNamingException
     * @throws SiloFrontendException
     */
    public SiloFrontend(String zooHost, String zooPort, int cacheSize) throws ZKNamingException, SiloFrontendException {
        this(zooHost, zooPort);
        responseCache = new ResponseCache(cacheSize);
    }

    /**
     * Creates a SiloFrontend that will contact the nameserver on zooHost:zooPort, which will initially contact /grpc/sauron/silo/replicaId.
     * Initializes the frontend's cache with size cacheSize.
     *
     * @param zooHost   The nameserver's host
     * @param zooPort   The nameserver's port
     * @param replicaId The id of the replica to be contacted
     * @param cacheSize Initial cache size
     * @throws ZKNamingException
     * @throws SiloFrontendException
     */
    public SiloFrontend(String zooHost, String zooPort, String replicaId, int cacheSize) throws ZKNamingException, SiloFrontendException {
        this(zooHost, zooPort, replicaId);
        responseCache = new ResponseCache(cacheSize);
    }

    /**
     * Connects to a random available replica
     *
     * @return A collection of available replicas
     * @throws ZKNamingException
     * @throws SiloFrontendException
     */
    public Collection<ZKRecord> connectToRandomReplica() throws ZKNamingException, SiloFrontendException {
        // listRecords
        Collection<ZKRecord> records = zkNaming.listRecords("/grpc/sauron/silo");
        ZKRecord recordChosen;
        if (!records.isEmpty()) {
            recordChosen = records.stream()
                    .skip(new Random().nextInt(records.size()))
                    .findFirst()
                    .orElseThrow(() -> new SiloFrontendException("No available server."));
        } else {
            throw new SiloFrontendException("No available server.");
        }

        // lookup
        ZKRecord record = zkNaming.lookup(recordChosen.getPath());
        String target = record.getURI();

        if (channel != null) {
            channel.shutdownNow();
        }
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = ReplicaManagerGrpc.newBlockingStub(channel);

        // Update our timestamp so that we always have the position of the replica we are contacting
        while (this.vectorTimestamp.size() < records.size()) {
            this.vectorTimestamp.add(0L);
        }

        return records;
    }

    /**
     * Forwards a ctrlPing request
     *
     * @param request The request to be forwarded
     * @return The response received
     */
    public CtrlPingResponse ctrlPing(CtrlPingRequest request) {
        return stub.ctrlPing(request);
    }

    /**
     * Forwards a ctrlClear request
     *
     * @param request The request to be forwarded
     * @return The response received
     */
    public CtrlClearResponse ctrlClear(CtrlClearRequest request) {
        return stub.ctrlClear(request);
    }

    /**
     * Forwards a ctrlInit request
     *
     * @param request The request to be forwarded
     * @return The response received
     */
    public CtrlInitResponse ctrlInit(CtrlInitRequest request) {
        return stub.ctrlInit(request);
    }

    /**
     * Forwards a camJoin request
     *
     * @param request The request to be forwarded
     * @return The response received
     * @throws SiloFrontendException
     */
    public CamJoinResponse camJoin(CamJoinRequest request) throws SiloFrontendException {
        ClientUpdateResponse response = stub.camJoin(ClientUpdateRequest.newBuilder()
                .setCamJoinRequest(request)
                .addAllPrevVectorTimestamp(this.vectorTimestamp)
                .build());

        if (response.hasCamJoinResponse()) {
            updateVectorTimestamp(response.getNewVectorTimestampList());
            return response.getCamJoinResponse();
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Forwards a camInfo request
     *
     * @param request The request to be forwarded
     * @return The response received or a cached response if the received one is older
     * @throws SiloFrontendException
     */
    public CamInfoResponse camInfo(CamInfoRequest request) throws SiloFrontendException {
        String command = "camInfo " + request.getCamera().getName();
        ClientQueryResponse response = ClientQueryResponse.newBuilder().build();
        try {
            response = stub.camInfo(ClientQueryRequest.newBuilder()
                    .setCamInfoRequest(request)
                    .addAllPrevVectorTimestamp(this.vectorTimestamp)
                    .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND && responseCache.getCameraResponse(command) != null) {
                return CamInfoResponse.newBuilder().setCamera(responseCache.getCameraResponse(command)).build();
            } else {
                throw e;
            }
        }

        if (response.hasCamInfoResponse()) {
            // If server timestamp is greater or equal than the client timestamp or if cache
            // doesnt have a value to that request returns the server response
            List<Long> responseTimestampCopy = new ArrayList<>(response.getNewVectorTimestampList());
            if (checkServerTimestamp(responseTimestampCopy)
                    || responseCache.getCameraResponse(command) == null) {
                updateVectorTimestamp(responseTimestampCopy);
                responseCache.addResponse(command, response.getCamInfoResponse().getCamera());
                return response.getCamInfoResponse();
            } else {
                return CamInfoResponse.newBuilder().setCamera(responseCache.getCameraResponse(command)).build();
            }
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Forwards a report request
     *
     * @param request The request to be forwarded
     * @return The response received
     * @throws SiloFrontendException
     */
    public ReportResponse report(ReportRequest request) throws SiloFrontendException {
        ClientUpdateResponse response = stub.report(ClientUpdateRequest.newBuilder()
                .setReportRequest(request)
                .addAllPrevVectorTimestamp(this.vectorTimestamp)
                .build());

        if (response.hasReportResponse()) {
            updateVectorTimestamp(response.getNewVectorTimestampList());
            return response.getReportResponse();
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Forwards a track request
     *
     * @param request The request to be forwarded
     * @return The response received or a cached response if the received one is older
     * @throws SiloFrontendException
     */
    public TrackResponse track(TrackRequest request) throws SiloFrontendException {
        ClientQueryResponse response = stub.track(ClientQueryRequest.newBuilder()
                .setTrackRequest(request)
                .addAllPrevVectorTimestamp(this.vectorTimestamp)
                .build());

        if (response.hasTrackResponse()) {
            String command = "";
            if (request.getType() == ObjectType.PERSON) {
                command = "track " + request.getId().getPersonId();
            } else if (request.getType() == ObjectType.CAR) {
                command = "track " + request.getId().getCarId();
            }

            // If server timestamp is greater or equal than the client timestamp or if cache
            // doesnt have a value to that request returns the server response
            if (checkServerTimestamp(response.getNewVectorTimestampList())
                    || responseCache.getObservationResponse(command) == null) {
                updateVectorTimestamp(response.getNewVectorTimestampList());
                responseCache.addResponse(command,
                        Collections.singletonList(response.getTrackResponse().getMostRecentObs()));
                return response.getTrackResponse();
            } else {
                return TrackResponse.newBuilder().setMostRecentObs(responseCache.getObservationResponse(command).get(0)).build();
            }
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Forwards a trackMatch request
     *
     * @param request The request to be forwarded
     * @return The response received or a cached response if the received one is older
     * @throws SiloFrontendException
     */
    public TrackMatchResponse trackMatch(TrackMatchRequest request) throws SiloFrontendException {
        ClientQueryResponse response = stub.trackMatch(ClientQueryRequest.newBuilder()
                .setTrackMatchRequest(request)
                .addAllPrevVectorTimestamp(this.vectorTimestamp)
                .build());

        if (response.hasTrackMatchResponse()) {
            String command = "track_match " + request.getPartialId() + " " + request.getType();

            // If server timestamp is greater or equal than the client timestamp or if cache
            // doesnt have a value to that request returns the server response
            if (checkServerTimestamp(response.getNewVectorTimestampList())
                    || responseCache.getObservationResponse(command) == null) {
                updateVectorTimestamp(response.getNewVectorTimestampList());
                responseCache.addResponse(command, response.getTrackMatchResponse().getUnorderedObsList());
                return response.getTrackMatchResponse();
            } else {
                return TrackMatchResponse.newBuilder().addAllUnorderedObs(responseCache.getObservationResponse(command)).build();
            }
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Forwards a trace request
     *
     * @param request The request to be forwarded
     * @return The response received or a cached response if the received one is older
     * @throws SiloFrontendException
     */
    public TraceResponse trace(TraceRequest request) throws SiloFrontendException {
        ClientQueryResponse response = stub.trace(ClientQueryRequest.newBuilder()
                .setTraceRequest(request)
                .addAllPrevVectorTimestamp(this.vectorTimestamp)
                .build());

        if (response.hasTraceResponse()) {
            String command = "";
            if (request.getType() == ObjectType.PERSON) {
                command = "trace " + request.getId().getPersonId();
            } else if (request.getType() == ObjectType.CAR) {
                command = "trace " + request.getId().getCarId();
            }

            // If server timestamp is greater or equal than the client timestamp or if cache
            // doesnt have a value to that request returns the server response
            if (checkServerTimestamp(response.getNewVectorTimestampList())
                    || responseCache.getObservationResponse(command) == null) {
                updateVectorTimestamp(response.getNewVectorTimestampList());
                responseCache.addResponse(command, response.getTraceResponse().getOrderedObsList());
                return response.getTraceResponse();
            } else {
                return TraceResponse.newBuilder().addAllOrderedObs(responseCache.getObservationResponse(command)).build();
            }
        } else {
            throw new SiloFrontendException("Unexpected response.");
        }
    }

    /**
     * Updates the Frontend vectorTimestamp according to the state of the contacted replica.
     *
     * @param newVectorTimestamp vectorTimestamp reflecting the state of the contacted replica
     */
    private void updateVectorTimestamp(List<Long> newVectorTimestamp) {
        for (int i = 0; i < newVectorTimestamp.size(); i++) {
            try {
                if (vectorTimestamp.get(i) < newVectorTimestamp.get(i)) {
                    vectorTimestamp.set(i, newVectorTimestamp.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
                vectorTimestamp.add(i, newVectorTimestamp.get(i));
            }
        }
    }

    private boolean checkServerTimestamp(List<Long> serverVectorTimestamp) {
        // Same size in both vector timestamps
        if (serverVectorTimestamp.size() > vectorTimestamp.size()) {
            this.vectorTimestamp.addAll(
                    Collections.nCopies(serverVectorTimestamp.size() - vectorTimestamp.size(), 0L));
        } else if (vectorTimestamp.size() > serverVectorTimestamp.size()) {
            serverVectorTimestamp.addAll(
                    Collections.nCopies(vectorTimestamp.size() - serverVectorTimestamp.size(), 0L));
        }

        // Checks if server has updates missing
        for (int i = 0; i < vectorTimestamp.size(); i++) {
            if (serverVectorTimestamp.get(i) < vectorTimestamp.get(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}

