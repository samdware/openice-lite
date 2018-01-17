package edu.upenn.cis.precise.openicelite.drivers.core;

/**
 * Allow an application to be notified when asynchronous events
 * (data arrives, error happens, etc.) occur
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface IDriverCallback {
    /**
     * Handle new data from driver as a byte array
     *
     * @param message data to be handled
     */
    void handleMessage(byte[] message);

    /**
     * Handle new data from driver as a string
     *
     * @param message data to be handled
     */
    void handleMessage(String message);
}
