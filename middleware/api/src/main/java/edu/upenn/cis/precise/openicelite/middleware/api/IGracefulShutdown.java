package edu.upenn.cis.precise.openicelite.middleware.api;

/**
 * Interface to handle graceful shutdown request
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface IGracefulShutdown {
    /**
     * Disconnect from broker
     */
    void disconnect();

    /**
     * Close and release all associated resource
     */
    void close();
}
