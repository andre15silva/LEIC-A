package pt.tecnico.sauron.silo;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;

public class SiloServerApp {
    static class ShutdownThread extends Thread {
        Server server;
        ZKNaming zkNaming;
        String path;
        String host;
        String port;

        public ShutdownThread(Server server, ZKNaming zkNaming, String path, String host, String port) {
            this.server = server;
            this.zkNaming = zkNaming;
            this.path = path;
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            server.shutdown();
            if (zkNaming != null) {
                try {
                    // remove
                    zkNaming.unbind(path, host, port);
                } catch (ZKNamingException zkNamingException) {
                    System.out.println(zkNamingException.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {
        System.out.println(SiloServerApp.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 6) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s zooHost zooPort replicaId host port gossip_interval%n", SiloServerApp.class.getName());
            return;
        }

        final String zooHost = args[0];
        final String zooPort = args[1];
        final String replicaId = args[2];
        final String path = String.format("/grpc/sauron/silo/%s", replicaId);
        final String host = args[3];
        final String port = args[4];
        final String gossipInterval = args[5];
        final String nReplicasToSend = args.length >= 7 ? args[6] : "-1";
        ZKNaming zkNaming = null;

        zkNaming = new ZKNaming(zooHost, zooPort);
        // publish
        zkNaming.rebind(path, host, port);

        // Create Silo
        final BindableService impl = new ReplicaManagerImpl(replicaId, zkNaming, gossipInterval, nReplicasToSend);

        // Create a new server to listen on port.
        Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

        // Start the server.
        server.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Create new thread where we wait for the user input.
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(server, zkNaming, path, host, port));

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }

}
