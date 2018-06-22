package edu.upenn.cis.precise.openicelite.middleware.api;

/**
 * Shutdown hook to handle graceful shutdown
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class ShutdownHook extends Thread {
    private final IGracefulShutdown client;

    public ShutdownHook(IGracefulShutdown client) {
        this.client = client;
    }

    @Override
    public void run() {
        client.disconnect();
        client.close();
    }
}
