package edu.upenn.cis.precise.openicelite.middleware.api;

/**
 * Allow an application to be notified when asynchronous events
 * (message arrives, connection lost, etc.) occur
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface IMiddlewareCallback {
    /**
     * Handle new message from middleware as a byte array
     *
     * @param topic the topic the message was delivered from
     * @param message the payload as a byte array
     */
    void handleMessage(String topic, byte[] message);

    /**
     * Handle new message from middleware as a string
     *
     * @param topic the topic the message was delivered from
     * @param message the payload as a string
     */
    void handleMessage(String topic, String message);
}
