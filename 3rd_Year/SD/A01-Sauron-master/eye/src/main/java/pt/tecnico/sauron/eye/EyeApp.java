package pt.tecnico.sauron.eye;

import com.google.type.LatLng;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class EyeApp {
    private static final Logger LOGGER = Logger.getLogger(EyeApp.class.getName());

    /**
     * Sends a list of observations to the server
     *
     * @param frontend The server's frontend
     * @param cam      The camera from where these observations come from
     * @param obs      The list of observations to be sent
     */
    private static void sendObservations(SiloFrontend frontend, CameraDto cam, List<ObservationDto> obs) {
        if (!obs.isEmpty()) {
            ReportRequest request = ReportRequest.newBuilder().setCameraName(cam.getName()).addAllObs(obs).build();
            try {
                frontend.report(request);
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    try {
                        frontend.connectToRandomReplica();
                        frontend.camJoin(CamJoinRequest.newBuilder().setCamera(cam).build());
                    } catch (SiloFrontendException | ZKNamingException e2) {
                        LOGGER.warning("Error: " + e2.getLocalizedMessage());
                    }
                    sendObservations(frontend, cam, obs);
                } else {
                    LOGGER.warning("Error: " + e.getLocalizedMessage());
                }
            } catch (SiloFrontendException e) {
                LOGGER.warning("Error: " + e.getLocalizedMessage());
            }
            obs.clear();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 5) {
            LOGGER.warning("Use: eye <zooHost> <zooPort> <camName> <lat> <long> [<replicaId>]");
            System.exit(-1);
        }

        SiloFrontend frontend = null;
        CameraDto cam = null;
        try {
            final String zooHost = args[0];
            final String zooPort = args[1];
            final String eyeName = args[2];
            final LatLng eyeCoords = LatLng.newBuilder().setLatitude(Double.parseDouble(args[3])).setLongitude(Double.parseDouble(args[4])).build();
            cam = CameraDto.newBuilder().setName(eyeName).setCoords(eyeCoords).build();
            final String replicaId = args.length >= 6 ? args[5] : "";
            frontend = !replicaId.equals("") ? new SiloFrontend(zooHost, zooPort, replicaId) : new SiloFrontend(zooHost, zooPort);

            // Start by registering with server
            final CamJoinRequest request = CamJoinRequest.newBuilder().setCamera(cam).build();
            frontend.camJoin(request);
            LOGGER.info("Successfully registered eye:\n>Name:\n" + eyeName + "\n>Coords:\n" + eyeCoords);
        } catch (ZKNamingException | SiloFrontendException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        } catch (StatusRuntimeException e) {
            LOGGER.severe("Failed to register eye. Error:\n" + e.getStatus().getDescription());
            System.exit(-1);
        } catch (NumberFormatException e) {
            LOGGER.severe("Not a valid number: " + e.getMessage());
            System.exit(-1);
        }

        List<ObservationDto> obs = new ArrayList<>();
        Scanner repl = new Scanner(System.in);
        while (repl.hasNextLine()) {
            try {
                String line = repl.nextLine();
                if (line.isEmpty()) {
                    // Line being empty is the same as a newline,
                    // as such, send the stored observations.
                    sendObservations(frontend, cam, obs);
                } else if (line.charAt(0) != '#') {
                    // First split the line into tokens (separator = ",")
                    String[] tokens = line.split(",");

                    // We can only handle two tokens
                    if (tokens.length == 2) {
                        ObservationDto.Builder builder = ObservationDto.newBuilder();
                        switch (tokens[0]) {
                            case "person":
                                builder.setType(ObjectType.PERSON);
                                builder.setId(Id.newBuilder()
                                        .setPersonId(Long.parseLong(tokens[1])).build());
                                break;

                            case "car":
                                builder.setType(ObjectType.CAR);
                                builder.setId(Id.newBuilder().setCarId(tokens[1]).build());
                                break;

                            case "zzz":
                                // Special instruction, just go to sleep and skip building the request
                                LOGGER.info("Going to sleep");
                                Thread.sleep(Long.parseLong(tokens[1]));
                                continue;

                            default:
                                LOGGER.warning("Invalid first token");
                        }

                        if (builder.getId().getIdCase() != Id.IdCase.ID_NOT_SET) {
                            // We were able to parse the observation, we can
                            // remember it
                            obs.add(builder.build());
                        } else {
                            LOGGER.warning("Invalid observation object type");
                        }
                    } else {
                        LOGGER.warning("Observation has wrong number of attributes");
                    }
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getStatus().getDescription());
            } catch (NumberFormatException e) {
                LOGGER.warning("Not a valid number: " + e.getMessage());
            }
        }

        // If input stops, send the last collected observations
        // and shutdown the connection
        sendObservations(frontend, cam, obs);
        LOGGER.info("Closing server");
        frontend.close();
    }
}
