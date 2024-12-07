package pt.tecnico.sauron.silo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.domain.Camera;
import pt.tecnico.sauron.silo.domain.Observation;
import pt.tecnico.sauron.silo.domain.exception.InvalidParameterException;
import pt.tecnico.sauron.silo.domain.exception.NotFoundException;
import pt.tecnico.sauron.silo.domain.exception.PrimaryKeyTakenException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static io.grpc.Status.*;
import static java.util.stream.Collectors.toList;

public class ReplicaManagerImpl extends ReplicaManagerGrpc.ReplicaManagerImplBase {
    private static final Logger LOGGER = Logger.getLogger(ReplicaManagerImpl.class.getName());
    private static final Silo silo = new Silo();
    private final Integer replicaId;
    private ArrayList<Long> vectorTimestamp = new ArrayList<>();
    private final ZKNaming zkNaming;
    private final int nReplicasToSend;

    private final Map<UpdatePair, Camera> cameraUpdates = new HashMap<>();
    private final Map<UpdatePair, Observation> observationUpdates = new HashMap<>();

    /**
     * Constructs a ReplicaManager.
     *
     * @param replicaId       The id of the replica.
     * @param zkNaming        The nameserver to be contacted.
     * @param gossipInterval  The interval between gossip operations triggered by this replica.
     * @param nReplicasToSend The maximum number of replicas that we query for updates.
     */
    public ReplicaManagerImpl(String replicaId, ZKNaming zkNaming, String gossipInterval, String nReplicasToSend) {
        this.replicaId = Integer.parseInt(replicaId);
        this.zkNaming = zkNaming;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        int queryInterval = Integer.parseInt(gossipInterval);
        this.nReplicasToSend = Integer.parseInt(nReplicasToSend);

        // Initialize timestamp
        this.vectorTimestamp.addAll(Collections.nCopies(Integer.parseInt(replicaId), 0L));

        // Initialize scheduler
        scheduler.scheduleAtFixedRate(this::getUpdates, 0, queryInterval, TimeUnit.SECONDS);
    }

    /**
     * Constructs the request's prevTimestamp, merging the replica's position with the current one.
     * This corresponds to part of the update's unique identifier.
     *
     * @param request The request
     * @return The request's prevTimestamp
     */
    public List<Long> newPrevTimestamp(ClientUpdateRequest request) {
        // Convert prevTimestamp to an ArrayList as protobuf's array doesn't support set operation after being build
        ArrayList<Long> newPrev = new ArrayList<>(request.getPrevVectorTimestampList());

        // Make sure we can edit our replicaId position
        while (newPrev.size() < replicaId) {
            newPrev.add(0L);
        }

        newPrev.set(replicaId - 1, vectorTimestamp.get(replicaId - 1));
        return newPrev;
    }

    /**
     * Queries a previously specified number of random available replicas for updates and applies them.
     */
    public void getUpdates() {
        LOGGER.info("Starting gossip with other replicas. Initial timestamp: " + vectorTimestamp);
        try {
            // Remove ourselves from the list of all records and shuffle it so we can get random replicas
            List<ZKRecord> records = zkNaming.listRecords("/grpc/sauron/silo").stream().filter((r) -> {
                return (Integer.parseInt(r.getURI().split(":")[1]) - 8080) != replicaId;
            }).collect(toList());

            Collections.shuffle(records);
            // If the user specified a valid number of replicas to send, set it
            // to choose the smaller number between that value and the full size
            // of the records list (to make sure we don't go over the maximum bounds)
            int size = records.size();
            if (this.nReplicasToSend >= 0) {
                size = Math.min(size, this.nReplicasToSend);
            }
            for (ZKRecord record : records.subList(0, size)) {
                String uri = record.getURI();

                // Create channel
                LOGGER.info("Gossip with: " + record.getPath());
                ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                ReplicaManagerGrpc.ReplicaManagerBlockingStub stub = ReplicaManagerGrpc.newBlockingStub(channel);

                // Send request
                QueryReplicaResponse response = stub.queryReplica(QueryReplicaRequest.newBuilder()
                        .addAllVectorTimestamp(vectorTimestamp).build());
                channel.shutdown();
                LOGGER.info("Received " + response.getUpdatesCount() + " updates with timestamp: " + response.getVectorTimestampList());

                // Fill our vectorTimestamp in case other replicas have joined in
                while (response.getVectorTimestampCount() > vectorTimestamp.size()) {
                    vectorTimestamp.add(0L);
                }

                // We need to convert from proto to java because proto does not allow mutable lists
                List<Update> updatesList = new ArrayList<>(response.getUpdatesList());

                // Sort the updatesList
                updatesList.sort((a, b) -> {
                    if (a.getUpdatePair().getReplicaId() == b.getUpdatePair().getReplicaId()) {
                        return (int) (a.getUpdatePair().getVectorTimestamp(a.getUpdatePair().getReplicaId() - 1) -
                                b.getUpdatePair().getVectorTimestamp(b.getUpdatePair().getReplicaId() - 1));
                    } else {
                        return a.getUpdatePair().getReplicaId() - b.getUpdatePair().getReplicaId();
                    }
                });

                for (Update update : updatesList) {
                    applyUpdate(update);
                }

                LOGGER.info("Finished gossip with " + record.getPath() + ". Resulting timestamp: " + vectorTimestamp);
            }
            LOGGER.info("Finished gossip with other replicas. Final timestamp: " + vectorTimestamp);
        } catch (StatusRuntimeException | ZKNamingException e) {
            LOGGER.warning(e.getLocalizedMessage());
        }
    }

    /**
     * Applies an update to the replica.
     *
     * @param update The update to be applied
     */
    public synchronized void applyUpdate(Update update) {
        int otherReplicaIndex = update.getUpdatePair().getReplicaId() - 1;
        List<Long> prevTimestamp = new ArrayList<>(update.getUpdatePair().getVectorTimestampList());

        if (vectorTimestamp.get(otherReplicaIndex).equals(prevTimestamp.get(otherReplicaIndex))) {
            try {
                switch (update.getObjectCase()) {
                    case CAMERA:
                        Camera cam = silo.camJoin(CamJoinRequest.newBuilder().setCamera(update.getCamera()).build());
                        addToUpdateLog(cam, new UpdatePair(update.getUpdatePair().getReplicaId(), prevTimestamp));
                        break;
                    case OBSERVATION:
                        Observation observation = silo.addObservation(update.getObservation());
                        List<Observation> observations = new ArrayList<>();
                        observations.add(observation);
                        addObsToUpdateLog(observations, update.getUpdatePair().getReplicaId(), prevTimestamp);
                }
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException | NotFoundException | PrimaryKeyTakenException e) {
                LOGGER.severe("Update corrupted.");
            }
        }
    }

    @Override
    public void queryReplica(QueryReplicaRequest request, StreamObserver<QueryReplicaResponse> responseObserver) {
        LOGGER.info("queryReplica (" + request + ")");
        List<Update> updatesToSend = new ArrayList<>();
        // We need to convert from proto's list to java because proto does not support mutable lists
        List<Long> otherReplicaTimestamp = new ArrayList<>(request.getVectorTimestampList());

        // Fill the other timestamp, as it can be smaller than ours
        while (vectorTimestamp.size() > otherReplicaTimestamp.size()) {
            otherReplicaTimestamp.add(0L);
        }

        try {
            // Check if any camera update is to be sent
            for (UpdatePair up : cameraUpdates.keySet()) {
                if (up.getPrevVectorTimestamp().get(up.getReplicaId() - 1) + 1 > otherReplicaTimestamp.get(up.getReplicaId() - 1)) {
                    updatesToSend.add(Update.newBuilder()
                            .setUpdatePair(pt.tecnico.sauron.silo.grpc.UpdatePair.newBuilder()
                                    .setReplicaId(up.getReplicaId())
                                    .addAllVectorTimestamp(up.getPrevVectorTimestamp())
                                    .build())
                            .setCamera(Silo.buildCamera(cameraUpdates.get(up))).build());
                }
            }

            // Check if any observation update is to be sent
            for (UpdatePair up : observationUpdates.keySet()) {
                if (up.getPrevVectorTimestamp().get(up.getReplicaId() - 1) + 1 > otherReplicaTimestamp.get(up.getReplicaId() - 1)) {
                    updatesToSend.add(Update.newBuilder()
                            .setUpdatePair(pt.tecnico.sauron.silo.grpc.UpdatePair.newBuilder()
                                    .setReplicaId(up.getReplicaId())
                                    .addAllVectorTimestamp(up.getPrevVectorTimestamp())
                                    .build())
                            .setObservation(Silo.buildObservation(observationUpdates.get(up))).build());
                }
            }

            responseObserver.onNext(QueryReplicaResponse.newBuilder()
                    .addAllUpdates(updatesToSend)
                    .addAllVectorTimestamp(vectorTimestamp)
                    .build());
            responseObserver.onCompleted();
            LOGGER.info("Sent " + updatesToSend.size() + " updates.");
        } catch (pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
            LOGGER.severe(e.getLocalizedMessage());
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void ctrlPing(CtrlPingRequest request, StreamObserver<CtrlPingResponse> responseObserver) {
        silo.ctrlPing(request, responseObserver);
    }

    @Override
    public void ctrlClear(CtrlClearRequest request, StreamObserver<CtrlClearResponse> responseObserver) {
        silo.ctrlClear(request, responseObserver);
    }

    @Override
    public void ctrlInit(CtrlInitRequest request, StreamObserver<CtrlInitResponse> responseObserver) {
        silo.ctrlInit(request, responseObserver);
    }

    @Override
    public void camJoin(ClientUpdateRequest request, StreamObserver<ClientUpdateResponse> responseObserver) {
        LOGGER.info("camJoin(" + request + ")");
        if (request.hasCamJoinRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                Camera cam = silo.camJoin(request.getCamJoinRequest());

                List<Long> newPrevTimestamp = newPrevTimestamp(request);
                addToUpdateLog(cam, new UpdatePair(replicaId, newPrevTimestamp));

                responseObserver.onNext(ClientUpdateResponse.newBuilder()
                        .setCamJoinResponse(CamJoinResponse.newBuilder().build())
                        .addAllNewVectorTimestamp(vectorTimestamp).build());
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            } catch (NotFoundException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(NOT_FOUND
                        .withDescription(e.getMessage()).asRuntimeException());
            } catch (PrimaryKeyTakenException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(ALREADY_EXISTS
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    @Override
    public void report(ClientUpdateRequest request, StreamObserver<ClientUpdateResponse> responseObserver) {
        LOGGER.info("report(" + request + ")");
        if (request.hasReportRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                List<Observation> obs = silo.report(request.getReportRequest());

                List<Long> newPrevTimestamp = newPrevTimestamp(request);
                addObsToUpdateLog(obs, replicaId, newPrevTimestamp);

                responseObserver.onNext(ClientUpdateResponse.newBuilder()
                        .setReportResponse(ReportResponse.newBuilder().build())
                        .addAllNewVectorTimestamp(vectorTimestamp).build());
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            } catch (NotFoundException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(NOT_FOUND
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    @Override
    public void camInfo(ClientQueryRequest request, StreamObserver<ClientQueryResponse> responseObserver) {
        LOGGER.info("camInfo(" + request + ")");
        if (request.hasCamInfoRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                Camera camera = silo.camInfo(request.getCamInfoRequest());
                responseObserver.onNext(ClientQueryResponse.newBuilder().setCamInfoResponse(
                        CamInfoResponse.newBuilder().setCamera(Silo.buildCamera(camera)).build()).addAllNewVectorTimestamp(vectorTimestamp).build());
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            } catch (NotFoundException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(NOT_FOUND
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    @Override
    public void track(ClientQueryRequest request, StreamObserver<ClientQueryResponse> responseObserver) {
        LOGGER.info("track(" + request + ")");
        if (request.hasTrackRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                Observation recentObservation = silo.track(request.getTrackRequest());
                if (recentObservation == null) {
                    responseObserver.onNext(ClientQueryResponse.newBuilder().setTrackResponse(
                            TrackResponse.getDefaultInstance())
                            .addAllNewVectorTimestamp(vectorTimestamp).build());
                } else {
                    responseObserver.onNext(ClientQueryResponse.newBuilder().setTrackResponse(
                            TrackResponse.newBuilder()
                                    .setMostRecentObs(Silo.buildObservation(recentObservation)).build())
                            .addAllNewVectorTimestamp(vectorTimestamp).build());
                }
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    @Override
    public void trackMatch(ClientQueryRequest request, StreamObserver<ClientQueryResponse> responseObserver) {
        LOGGER.info("trackMatch(" + request + ")");
        if (request.hasTrackMatchRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                List<Observation> recentObs = silo.trackMatch(request.getTrackMatchRequest());
                var trackMatchResponse = TrackMatchResponse.newBuilder();
                for (Observation ob : recentObs) {
                    trackMatchResponse.addUnorderedObs(Silo.buildObservation(ob));
                }
                ClientQueryResponse response = ClientQueryResponse.newBuilder().addAllNewVectorTimestamp(vectorTimestamp)
                        .setTrackMatchResponse(trackMatchResponse.build()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    @Override
    public void trace(ClientQueryRequest request, StreamObserver<ClientQueryResponse> responseObserver) {
        LOGGER.info("trace(" + request + ")");
        if (request.hasTraceRequest() && request.getPrevVectorTimestampCount() != 0) {
            try {
                List<Observation> sortedObservations = silo.trace(request.getTraceRequest());
                var traceResponse = TraceResponse.newBuilder();
                for (Observation obs : sortedObservations) {
                    traceResponse.addOrderedObs(Silo.buildObservation(obs));
                }
                ClientQueryResponse response = ClientQueryResponse.newBuilder().addAllNewVectorTimestamp(vectorTimestamp)
                        .setTraceResponse(traceResponse.build()).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (InvalidParameterException | pt.tecnico.sauron.silo.exception.InvalidParameterException e) {
                LOGGER.warning(e.getMessage());
                responseObserver.onError(INVALID_ARGUMENT
                        .withDescription(e.getMessage()).asRuntimeException());
            }
        }

    }

    /**
     * Adds a camera update to the replica's log
     *
     * @param camera     The camera
     * @param updatePair The update's key
     */
    private synchronized void addToUpdateLog(Camera camera, UpdatePair updatePair) {
        LOGGER.info("Adding camera update. Initial timestamp: " + vectorTimestamp);

        // Create update and insert it
        cameraUpdates.put(updatePair, camera);

        // Update vectorTimestamp
        vectorTimestamp.set(updatePair.getReplicaId() - 1, vectorTimestamp.get(updatePair.getReplicaId() - 1) + 1);

        LOGGER.info("Added camera update. Final timestamp: " + vectorTimestamp);
    }

    /**
     * Adds a list of observation updates to the replica's log
     *
     * @param observations  The list of observations
     * @param replicaId     The update's replicaId
     * @param prevTimestamp The initial update's prevTimestamp
     */
    private synchronized void addObsToUpdateLog(List<Observation> observations, Integer replicaId, List<Long> prevTimestamp) {
        for (Observation observation : observations) {
            LOGGER.info("Adding observation update. Initial timestamp: " + vectorTimestamp);

            // Create update and insert it
            UpdatePair updatePair = new UpdatePair(replicaId, new ArrayList<>(prevTimestamp));
            observationUpdates.put(updatePair, observation);

            // Update vectorTimestamp
            vectorTimestamp.set(replicaId - 1, vectorTimestamp.get(replicaId - 1) + 1);

            // Update prevTimestamp
            prevTimestamp.set(replicaId - 1, prevTimestamp.get(replicaId - 1) + 1);

            LOGGER.info("Added observation update. Final timestamp: " + vectorTimestamp);
        }
    }

}
