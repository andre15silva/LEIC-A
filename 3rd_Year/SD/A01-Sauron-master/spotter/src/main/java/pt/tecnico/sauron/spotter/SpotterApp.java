package pt.tecnico.sauron.spotter;


import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SpotterApp {
    private static final String EXIT_CMD = "exit";
    private static final String PING_CMD = "ping";
    private static final String CLEAR_CMD = "clear";
    private static final String INIT_CMD = "init";
    private static final String HELP_CMD = "help";
    private static final String SPOT_CMD = "spot";
    private static final String TRAIL_CMD = "trail";
    private static final String INFO_CMD = "info";

    /**
     * Parses a command and handles the communication with the server
     *
     * @param line     The command to be executed, with its arguments
     * @param frontend The frontend of the server
     * @return True if the client should continue executing, False if the client should terminate
     */
    public static boolean parser(String line, SiloFrontend frontend) {
        try {
            // exit
            if (EXIT_CMD.equals(line)) {
                return false;
            }

            // ping cmd
            if (PING_CMD.equals(line)) {
                CtrlPingRequest request = CtrlPingRequest.newBuilder().setName("spotter").build();
                CtrlPingResponse response = frontend.ctrlPing(request);
                System.out.println(response);
                return true;
            }

            // clear cmd
            if (CLEAR_CMD.equals(line)) {
                frontend.ctrlClear(CtrlClearRequest.getDefaultInstance());
                System.out.println("Clear executed.");
                return true;
            }

            // init cmd
            if (INIT_CMD.equals(line)) {
                frontend.ctrlInit(CtrlInitRequest.getDefaultInstance());
                System.out.println("Init executed.");
                return true;
            }

            // help
            if (HELP_CMD.equals(line)) {
                System.out.print("Spotter.\n" +
                        "\n" +
                        "Commands:\n" +
                        "  info         Searches for the location of a camera.\n" +
                        "  spot         Searches observations of an object/person given it's full or partial id.\n" +
                        "  trail        Searches the path of an object/person given it's full id.\n" +
                        "  ping         Pings the server.\n" +
                        "  clear        Clears the server's database.\n" +
                        "  init         Populates the server's database with example instances of the domain.\n" +
                        "  help         Prints information about available commands and their respective usage.\n" +
                        "  exit         Terminates the program execution.\n" +
                        "\n" +
                        "Usage:\n" +
                        "  info <cam_name>\n" +
                        "  spot <type> <fullId>\n" +
                        "  spot <type> <partialId>\n" +
                        "  trail <type> <fullId>\n" +
                        "  ping\n" +
                        "  clear\n" +
                        "  init\n" +
                        "  help\n" +
                        "  exit\n");
                return true;
            }

            // info
            if (line.startsWith(INFO_CMD)) {
                String[] infoArgs = line.split(" ");

                // Check arguments
                if (infoArgs.length != 2) {
                    System.out.println(String.format("Command info takes 1 argument, not %d.", infoArgs.length - 1));
                    return true;
                }

                // request the resources from the server and present them to the user
                CamInfoRequest request = CamInfoRequest.newBuilder()
                        .setCamera(CameraDto.newBuilder().setName(infoArgs[1]).build()).build();
                CamInfoResponse response = frontend.camInfo(request);
                if (response.isInitialized() && response.hasCamera()) {
                    System.out.println("Camera: " + response.getCamera().getName()
                            + ", Location: Lat(" + response.getCamera().getCoords().getLatitude() + ") "
                            + " Long(" + response.getCamera().getCoords().getLongitude() + ") ");
                } else {
                    System.out.println("No camera was found.");
                }

                return true;
            }

            // spot
            if (line.startsWith(SPOT_CMD)) {
                String[] spotArgs = line.split(" ");

                // Check arguments
                if (spotArgs.length != 3) {
                    System.out.println(String.format("Command spot takes 2 arguments, not %d.", spotArgs.length - 1));
                    return true;
                } else if (!(spotArgs[1].equals("car") || spotArgs[1].equals("person"))) {
                    System.out.println("Invalid type argument.");
                    return true;
                }

                // Request the resources from the server and present them to the user
                if (spotArgs[2].contains("*")) {
                    // Partial id search
                    if (spotArgs[1].equals("car")) {
                        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR)
                                .setPartialId(spotArgs[2]).build();
                        TrackMatchResponse response = frontend.trackMatch(request);
                        printTrackMatchResponse(response);
                    } else if (spotArgs[1].equals("person")) {
                        TrackMatchRequest request = TrackMatchRequest.newBuilder().setType(ObjectType.PERSON)
                                .setPartialId(spotArgs[2]).build();
                        TrackMatchResponse response = frontend.trackMatch(request);
                        printTrackMatchResponse(response);
                    }
                } else {
                    // Exact id search
                    if (spotArgs[1].equals("car")) {
                        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.CAR)
                                .setId(Id.newBuilder().setCarId(spotArgs[2]).build()).build();
                        TrackResponse response = frontend.track(request);
                        printTrackResponse(response);
                    } else if (spotArgs[1].equals("person")) {
                        TrackRequest request = TrackRequest.newBuilder().setType(ObjectType.PERSON)
                                .setId(Id.newBuilder().setPersonId(Long.parseLong(spotArgs[2]))).build();
                        TrackResponse response = frontend.track(request);
                        printTrackResponse(response);
                    }
                }

                return true;
            }

            // trail
            if (line.startsWith(TRAIL_CMD)) {
                String[] trailArgs = line.split(" ");

                // Check arguments
                if (trailArgs.length != 3) {
                    System.out.println(String.format("Command trail takes 2 arguments, not %d.", trailArgs.length - 1));
                    return true;
                } else if (!(trailArgs[1].equals("car") || trailArgs[1].equals("person"))) {
                    System.out.println("Invalid type argument.");
                    return true;
                }

                // Request the resources from the server and present them to the user
                if (trailArgs[1].equals("car")) {
                    TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.CAR)
                            .setId(Id.newBuilder().setCarId(trailArgs[2])).build();
                    TraceResponse response = frontend.trace(request);
                    printTraceResponse(response);
                } else if (trailArgs[1].equals("person")) {
                    TraceRequest request = TraceRequest.newBuilder().setType(ObjectType.PERSON)
                            .setId(Id.newBuilder().setPersonId(Long.parseLong(trailArgs[2]))).build();
                    TraceResponse response = frontend.trace(request);
                    printTraceResponse(response);
                }

                return true;
            }

        } catch (NumberFormatException | SiloFrontendException e) {
            System.out.println(e.getMessage());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                try {
                    frontend.connectToRandomReplica();
                    parser(line, frontend);
                } catch (ZKNamingException | SiloFrontendException e2) {
                    System.out.println(e2.getMessage());
                    return false;
                }
            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println(SpotterApp.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s zooHost zooPort [replicaId]%n", SpotterApp.class.getName());
            return;
        }
        final String zooHost = args[0];
        final String zooPort = args[1];
        final int cacheSize = args.length >= 3 ? Integer.parseInt(args[2]) : 2048;
        final String replicaId = args.length >= 4 ? args[3] : "";

        try (SiloFrontend frontend = !replicaId.equals("") ? new SiloFrontend(zooHost, zooPort, replicaId, cacheSize) :
                new SiloFrontend(zooHost, zooPort, cacheSize); Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine();
                if (!parser(line, frontend)) break;
            }

        } catch (ZKNamingException | SiloFrontendException e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Closing...");
        }
    }

    /**
     * Displays a trace response to the user
     *
     * @param response The response to be displayed
     */
    private static void printTraceResponse(TraceResponse response) {
        if (response.isInitialized() && !response.getOrderedObsList().isEmpty()) {
            printObservationList(response.getOrderedObsList());
        } else {
            System.out.println("No observations were found.");
        }
    }

    /**
     * Displays a track response to the user
     *
     * @param response The response to be displayed
     */
    private static void printTrackResponse(TrackResponse response) {
        if (response.isInitialized() && response.hasMostRecentObs()) {
            printObservation(response.getMostRecentObs());
        } else {
            System.out.println("No observation was found.");
        }
    }

    /**
     * Displays a track match response to the user
     *
     * @param response The response to be displayed
     */
    private static void printTrackMatchResponse(TrackMatchResponse response) {
        if (response.isInitialized() && !response.getUnorderedObsList().isEmpty()) {
            List<ObservationDto> orderedObs = response.getUnorderedObsList().stream()
                    .sorted((j, i) -> {
                        if (i.getId() == j.getId()) {
                            return Timestamps.comparator().compare(i.getTimeStamp(), j.getTimeStamp());
                        } else if (i.getType() == ObjectType.CAR && j.getType() == ObjectType.CAR) {
                            return j.getId().getCarId().compareTo(i.getId().getCarId());
                        } else if (i.getType() == ObjectType.PERSON && j.getType() == ObjectType.PERSON) {
                            return Long.compare(j.getId().getPersonId(), i.getId().getPersonId());
                        } else if (i.getType() == ObjectType.PERSON) {
                            return 1;
                        } else {
                            return -1;
                        }
                    })
                    .collect(Collectors.toList());
            printObservationList(orderedObs);
        } else {
            System.out.println("No observations were found.");
        }

    }

    /**
     * Displays a list of observations to the user
     *
     * @param orderedObs The ordered list of observations to be displayed
     */
    private static void printObservationList(List<ObservationDto> orderedObs) {
        for (ObservationDto o : orderedObs) {
            printObservation(o);
        }
    }

    /**
     * Displays an observation to the user
     *
     * @param o The observation to be displayed
     */
    private static void printObservation(ObservationDto o) {
        if (o.getType() == ObjectType.CAR) {
            System.out.println(String.format("car,%s,%s,%s,%s,%s",
                    o.getId().getCarId(),
                    Instant.ofEpochSecond(o.getTimeStamp().getSeconds()).atZone(ZoneId.of("Europe/Lisbon")).toLocalDateTime(),
                    o.getCamera().getName(),
                    o.getCamera().getCoords().getLatitude(), o.getCamera().getCoords().getLongitude()));
        } else if (o.getType() == ObjectType.PERSON) {
            System.out.println(String.format("person,%s,%s,%s,%s,%s",
                    o.getId().getPersonId(),
                    Instant.ofEpochSecond(o.getTimeStamp().getSeconds()).atZone(ZoneId.of("Europe/Lisbon")).toLocalDateTime(),
                    o.getCamera().getName(),
                    o.getCamera().getCoords().getLatitude(), o.getCamera().getCoords().getLongitude()));
        }
    }

}