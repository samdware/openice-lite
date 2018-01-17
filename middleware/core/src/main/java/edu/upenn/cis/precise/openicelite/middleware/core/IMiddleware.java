package edu.upenn.cis.precise.openicelite.middleware.core;

import edu.upenn.cis.precise.openicelite.drivers.core.DeviceInfo;

import java.util.HashMap;

/**
 * Interface for all middleware implementations
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface IMiddleware extends IGracefulShutdown {
    /**
     * Perform some initializing tasks if needed
     *
     * @param options initializing options
     */
    void init(HashMap<String, Object> options);

    /**
     * Initiate a connection to server (for centralized distribution) or peer
     * (for decentralized distribution). Connection is keep alive until disconnect
     * is called.
     *
     * @param address centralized server's address or peer's address
     * @param name    identified client name (must be unique within the system)
     * @param options additional connecting options
     */
    void connect(String address, String name, HashMap<String, Object> options);

    /**
     * Determine if the middleware is currently connect to server or peers
     *
     * @return true if connected, false otherwise.
     */
    boolean isConnected();

    /**
     * Blocking call to wait until middleware is connected to server or peers
     */
    void waitConnected();

    /**
     * Subscribe to a topic, which may include addition options (such as QoS)
     *
     * @param topic   the topic to subscribe to
     * @param options additional subscribe options
     * @param callback the class to callback for related events
     */
    void subscribe(String topic, HashMap<String, Object> options, IMiddlewareCallback callback);

    /**
     * Unsubscribe to a topic, which may include addition options
     *
     * @param topic the topic to unsubscribe from
     */
    void unsubscribe(String topic);

    /**
     * Publish a message to a topic on the server
     *
     * @param topic   the topic to deliver the message to
     * @param message the byte array to use as the message
     * @param options additional publishing options
     */
    void publish(String topic, byte[] message, HashMap<String, Object> options);

    /**
     * Publish a message to a topic on the server
     *
     * @param topic   the topic to deliver the message to
     * @param message the string to use as the message
     * @param options additional publishing options
     */
    void publish(String topic, String message, HashMap<String, Object> options);

    /**
     * Publish a device message to the server
     *
     * @param deviceId the device ID generates the message
     * @param message  the byte array to use as the message
     * @param options  additional publishing options
     */
    void publishId(String deviceId, byte[] message, HashMap<String, Object> options);

    /**
     * Publish a device message to the server
     *
     * @param deviceId the device ID generates the message
     * @param message  the string to use as the message
     * @param options  additional publishing options
     */
    void publishId(String deviceId, String message, HashMap<String, Object> options);

    /**
     * Set the callback listener to use for events that happen asynchronously
     *
     * @param callback the class to callback for related events
     */
    void setCallback(IMiddlewareCallback callback);

    /**
     * Add information about device connected to the dongle
     *
     * @param info device information
     */
    void addDevice(DeviceInfo info);

    /**
     * Remove device from dongle
     *
     * @param deviceId device ID as a string
     */
    void removeDevice(String deviceId);
}
