package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.exceptions.SiloFrontendException;
import pt.tecnico.sauron.silo.grpc.CtrlPingRequest;
import pt.tecnico.sauron.silo.grpc.CtrlPingResponse;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.Scanner;

public class SiloClientApp {
    private static final String EXIT_CMD = "exit";
    private static final String PING_CMD = "ping";

    public static void main(String[] args) {
        System.out.println(SiloClientApp.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s zooHost zooPort%n", SiloClientApp.class.getName());
            return;
        }

        final String zooHost = args[0];
        final String zooPort = args[1];

        try (SiloFrontend frontend = new SiloFrontend(zooHost, zooPort); Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                try {
                    String line = scanner.nextLine();

                    // exit
                    if (EXIT_CMD.equals(line))
                        break;

                    // get name
                    if (PING_CMD.equals(line)) {
                        CtrlPingRequest request = CtrlPingRequest.newBuilder().setName("client").build();
                        CtrlPingResponse response = frontend.ctrlPing(request);
                        System.out.println(response);
                    }

                } catch (StatusRuntimeException e) {
                    System.out.println(e.getStatus().getDescription());
                }
            }
        } catch (ZKNamingException | SiloFrontendException e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Closing...");
        }

    }

}
