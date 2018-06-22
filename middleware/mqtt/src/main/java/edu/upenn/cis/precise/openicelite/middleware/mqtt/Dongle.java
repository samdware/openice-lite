package edu.upenn.cis.precise.openicelite.middleware.mqtt;

import edu.upenn.cis.precise.openicelite.iomt.api.DeviceInfo;
import edu.upenn.cis.precise.openicelite.middleware.api.IMiddleware;
import edu.upenn.cis.precise.openicelite.middleware.api.IMiddlewareCallback;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.type.DongleInfo;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.type.TopicHandler;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.util.SSLUtil;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enable an application to communicate with OpenICE-lite MQTT broker
 * <p>
 * The application can either be a dongle loaded with multiple medical devices or
 * a client app to collect data from medical devices.
 * <p>
 * Available options for initialization:
 * - "broker":              full address to the main broker
 *                              (e.g., ssl://hostname:port)
 * - "brokers":             fail-over addresses, separated with comma
 * - "connection_timeout":  timeout for connecting to broker
 * - "retry_interval":      retry interval if lost connection
 * - "alive_interval":      alive interval for MQTT connection
 * - "qos":                 MQTT QoS settings (0,1,2)
 * - "project_name":        project name will be used as a base topic separation
 *                              (default to DEFAULT)
 * - "report_interval":     interval for which the dongle send report message to MapManager
 * - "username":            username to login broker
 * - "password":            password to login broker
 * - "report_interval":     interval for which the dongle send report message to MapManager
 * - "ca_cert_file":        absolute path to CA certification for TLS connection
 * - "client_cert_file":    absolute path to client certification for TLS connection
 *                              (if broker requires client cert)
 * - "client_key_file":     absolute path to client private key for TLS connection
 * - "key_password":        password to unlock client private key if needed
 * - "aes_key":             AES secret key to encrypt/decrypt the message before publish to MQTT broker
 *                              (default to disable)
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
public class Dongle implements IMiddleware, MqttCallback {
    private static final Logger logger = LogManager.getLogger(Dongle.class);

    private static final int MAX_IN_FLIGHT = 1000;

    private static final int MIN_AES_KEY_LENGTH = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 16;

    private final String dongleId;

    // Overwritten-able configuration
    // -- MQTT broker address (main and fail-over)
    private String broker = "tcp://broker.hivemq.com:1883";
    private String[] brokers = {"tcp://broker.hivemq.com:1883"};
    // -- broker connection timeout (seconds)
    private int connectionTimeout = 10;
    // -- broker connection retry interval (seconds)
    private int retryInterval = 15;
    // -- broker keep alive interval (seconds)
    private int aliveInterval = 60;
    // -- QoS setting
    private int qos = 1;
    // -- project name (can be used to separate environment for different projects
    private String projectName = "DEFAULT";
    // -- interval to ping Map Manager (in seconds)
    private int reportInterval = 30;

    // Additional information from properties file
    private boolean useAuthentication = false;
    private String username = null;
    private String password = null;

    private boolean useSSL = false;
    private String caCertFile = null;
    private String clientCertFile = null;
    private String clientKeyFile = null;
    private String keyPassword = null;

    // MQTT
    private TopicHandler topicHandler = new TopicHandler();
    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private IMiddlewareCallback callback;

    // Dongle information
    private AtomicBoolean isRunning = new AtomicBoolean(true);
    private DongleInfo info;
    private Thread pinger;

    // AES encryption
    private boolean encryptMessage = false;
    private SecretKeySpec aesKey = null;

    /**
     * Dongle constructor
     *
     * @param dongleId dongle ID (UUID)
     */
    public Dongle(String dongleId) {
        this.dongleId = dongleId;
        this.info = new DongleInfo(dongleId);
    }

    /**
     * Perform some initializing tasks if needed
     *
     * @param options initializing options
     */
    @Override
    public void init(HashMap<String, Object> options) {
        logger.info("Initializing Dongle MQTT...");

        // Load options
        if (options != null) {
            if (options.containsKey("broker")) {
                broker = (String) options.get("broker");
            }
            if (options.containsKey("brokers")) {
                brokers = ((String) options.get("brokers")).split(",");
            }
            if (options.containsKey("connection_timeout")) {
                connectionTimeout = (int) options.get("connection_timeout");
            }
            if (options.containsKey("retry_interval")) {
                retryInterval = (int) options.get("retry_interval");
            }
            if (options.containsKey("alive_interval")) {
                aliveInterval = (int) options.get("alive_interval");
            }
            if (options.containsKey("qos")) {
                qos = (int) options.get("qos");
            }
            if (options.containsKey("project_name")) {
                projectName = (String) options.get("project_name");
            }
            if (options.containsKey("report_interval")) {
                reportInterval = (int) options.get("report_interval");
            }
            if (options.containsKey("username") && options.containsKey("password")) {
                useAuthentication = true;
                username = (String) options.get("username");
                password = (String) options.get("password");
            }
            if (options.containsKey("ca_cert_file")) {
                useSSL = true;
                caCertFile = (String) options.get("ca_cert_file");
                clientCertFile = (String) options.get("client_cert_file");
                clientKeyFile = (String) options.get("client_key_file");
                keyPassword = (String) options.get("key_password");
            }
            if (options.containsKey("aes_key")) {
                String key = (String) options.get("aes_key");
                if (key.length() < MIN_AES_KEY_LENGTH) {
                    logger.error("Invalid AES key: " + key);
                    throw new IllegalArgumentException("Invalid AES key");
                }

                try {
                    MessageDigest digester = MessageDigest.getInstance("SHA-256");
                    digester.update(key.getBytes("UTF-8"));
                    byte[] digest = digester.digest();
                    aesKey = new SecretKeySpec(digest, "AES");
                } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
                    logger.error("Failed to initialize AES encryption!", ex);
                    throw new IllegalArgumentException("Failed to AES encryption - " + ex.getMessage());
                }

                encryptMessage = true;
            }
        }

        // Finalize connect option
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setConnectionTimeout(connectionTimeout);
        mqttConnectOptions.setKeepAliveInterval(aliveInterval);
        mqttConnectOptions.setMaxInflight(MAX_IN_FLIGHT);
        if (brokers != null) mqttConnectOptions.setServerURIs(brokers);

        if (useAuthentication) {
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());
        }
        if (useSSL) {
            try {
                mqttConnectOptions.setSocketFactory(SSLUtil.getSocketFactory(caCertFile,
                        clientCertFile, clientKeyFile, keyPassword));
            } catch (Exception ex) {
                logger.error("Failed to initialize SSLSocket!", ex);
                throw new IllegalArgumentException("Failed to initialize SSLSocket - " + ex.getMessage());
            }
        }
    }

    /**
     * Initiate a connection to server (for centralized distribution) or peer
     * (for decentralized distribution). Connection is keep alive until disconnect
     * is called.
     * <p>
     * Dongle never exits and keeps retrying until connected.
     * <p>
     * This implementation will always ignore name and options (configuration should be
     * set during init).
     *
     * @param address centralized server's address or peer's address
     * @param name    identified client name (must be unique within the system)
     * @param options additional connecting options
     */
    @Override
    public void connect(String address, String name, HashMap<String, Object> options) {
        if (address != null) broker = address;
        String brokerAddress = (brokers != null) ? Arrays.toString(brokers) : broker;

        while (!isConnected.get()) {
            try {
                if (mqttClient == null) {
                    MqttDefaultFilePersistence dataStore =
                            new MqttDefaultFilePersistence("storage/" + projectName);
                    mqttClient = new MqttAsyncClient(broker, dongleId, dataStore);
                    mqttClient.setCallback(this);
                } else {
                    // Wait for retry if needed
                    Thread.sleep(retryInterval * 1000);
                }

                // Connect to broker
                logger.info("Connecting to broker at " + brokerAddress + " as " + dongleId + "...");
                IMqttToken connectToken = mqttClient.connect(mqttConnectOptions);
                connectToken.waitForCompletion();
                logger.info("Dongle is connected to broker!");

                // Start pinger if needed
                if (pinger == null) {
                    pinger = new MapManagerPinger(this);
                    pinger.start();
                }

                isConnected.set(true);
                isRunning.set(true);
            } catch (Exception ex) {
                isConnected.set(false);
                logger.error("Failed to connect to MQTT broker", ex);
                logger.info("Retry in " + retryInterval + " seconds...");
            }
        }
    }

    /**
     * Disconnect from server or network
     */
    @Override
    public void disconnect() {
        if (isConnected.get()) {
            try {
                logger.info("Disconnecting from broker...");
                IMqttToken disconnectToken = mqttClient.disconnect(null, null);
                disconnectToken.waitForCompletion(1000);
                isConnected.set(false);
            } catch (Exception ex) {
                logger.error("Failed to disconnect from MQTT broker", ex);
                isConnected.set(false);
            }
        }
    }

    /**
     * Close and release all associated resource
     */
    @Override
    public void close() {
        disconnect();
        isRunning.set(false);
        pinger = null;
    }

    /**
     * Determine if the middleware is currently connect to server or peers
     *
     * @return true if connected, false otherwise.
     */
    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * Blocking call to wait until middleware is connected to server or peers
     */
    @Override
    public void waitConnected() {
        while (!isConnected.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }

    /**
     * Subscribe to a topicHandler, which may include addition options (such as QoS)
     *
     * @param topic    the topicHandler to subscribe to
     * @param options  additional subscribe options
     * @param callback the class to callback for related events
     */
    @Override
    public void subscribe(String topic, HashMap<String, Object> options, IMiddlewareCallback callback) {
        try {
            if (callback != null) {
                this.callback = callback;
            }

            logger.info("Subscribing to " + topic + "...");
            IMqttToken subscribeToken = mqttClient.subscribe(topic, qos, null, null);
            subscribeToken.waitForCompletion();
        } catch (Exception ex) {
            logger.error("Failed to subscribe to topicHandler: " + topic, ex);
        }
    }

    /**
     * Unsubscribe to a topicHandler, which may include addition options
     *
     * @param topic the topicHandler to unsubscribe from
     */
    @Override
    public void unsubscribe(String topic) {
        try {
            callback = null;
            logger.info("Un-subscribing to " + topic + "...");
            IMqttToken unsubscribeToken = mqttClient.unsubscribe(topic);
            unsubscribeToken.waitForCompletion();
        } catch (Exception ex) {
            logger.error("Failed to un-subscribe to topicHandler: " + topic, ex);
        }
    }

    /**
     * Publish a message to a topicHandler on the server
     *
     * @param topic   the topicHandler to deliver the message to
     * @param message the byte array to use as the message
     * @param options additional publishing options
     */
    @Override
    public void publish(String topic, byte[] message, HashMap<String, Object> options) {
        if (mqttClient != null) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Publishing message to broker...");
                }
                MqttMessage mqttMessage;
                if (!encryptMessage || (options != null && options.containsKey("disable_aes"))) {
                    mqttMessage = new MqttMessage(message);
                } else {
                    mqttMessage = new MqttMessage(encryptMessage(message));
                }
                mqttMessage.setQos(qos);
                mqttClient.publish(topic, mqttMessage, null, null);
            } catch (MqttException ex) {
                if (ex.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED
                        || ex.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
                    logger.error("Failed to publish message to broker - connection lost!");
                    connectionLost(null);
                }
            } catch (Exception ex) {
                logger.error("Failed to publish message to broker", ex);
            }
        }
    }

    /**
     * Publish a message to a topicHandler on the server
     *
     * @param topic   the topicHandler to deliver the message to
     * @param message the string to use as the message
     * @param options additional publishing options
     */
    @Override
    public void publish(String topic, String message, HashMap<String, Object> options) {
        if (mqttClient != null) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Publishing message to broker: " + message);
                }
                MqttMessage mqttMessage;
                if (!encryptMessage || (options != null && options.containsKey("disable_aes"))) {
                    mqttMessage = new MqttMessage(message.getBytes());
                } else {
                    mqttMessage = new MqttMessage(encryptMessage(message.getBytes()));
                }
                mqttMessage.setQos(qos);
                mqttClient.publish(topic, mqttMessage, null, null);
            } catch (MqttException ex) {
                if (ex.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED
                        || ex.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
                    logger.error("Failed to publish message to broker - connection lost!");
                    connectionLost(null);
                }
            } catch (Exception ex) {
                logger.error("Failed to publish message to broker", ex);
            }
        }
    }

    /**
     * Publish a device message to the server
     *
     * @param deviceId the device ID generates the message
     * @param message  the byte array to use as the message
     * @param options  additional publishing options
     */
    @Override
    public void publishId(String deviceId, byte[] message, HashMap<String, Object> options) {
        if (mqttClient != null) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Publishing message to broker...");
                }
                MqttMessage mqttMessage;
                if (!encryptMessage || (options != null && options.containsKey("disable_aes"))) {
                    mqttMessage = new MqttMessage(message);
                } else {
                    mqttMessage = new MqttMessage(encryptMessage(message));
                }
                mqttMessage.setQos(qos);
                mqttClient.publish(topicHandler.getDataTopic(projectName, dongleId, deviceId),
                        mqttMessage, null, null);
            } catch (MqttException ex) {
                if (ex.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED
                        || ex.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
                    logger.error("Failed to publish message to broker - connection lost!");
                    connectionLost(null);
                }
            } catch (Exception ex) {
                logger.error("Failed to publish message to broker", ex);
            }
        }
    }

    /**
     * Publish a device message to the server
     *
     * @param deviceId the device ID generates the message
     * @param message  the string to use as the message
     * @param options  additional publishing options
     */
    @Override
    public void publishId(String deviceId, String message, HashMap<String, Object> options) {
        if (mqttClient != null) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Publishing message to broker: " + message);
                }
                MqttMessage mqttMessage;
                if (!encryptMessage || (options != null && options.containsKey("disable_aes"))) {
                    mqttMessage = new MqttMessage(message.getBytes());
                } else {
                    mqttMessage = new MqttMessage(encryptMessage(message.getBytes()));
                }
                mqttMessage.setQos(qos);
                mqttClient.publish(topicHandler.getDataTopic(projectName, dongleId, deviceId),
                        mqttMessage, null, null);
            } catch (MqttException ex) {
                if (ex.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED
                        || ex.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
                    logger.error("Failed to publish message to broker - connection lost!");
                    connectionLost(null);
                }
            } catch (Exception ex) {
                logger.error("Failed to publish message to broker", ex);
            }
        }
    }

    /**
     * Set the callback listener to use for events that happen asynchronously
     *
     * @param callback the class to callback for related events
     */
    @Override
    public void setCallback(IMiddlewareCallback callback) {
        if (callback != null) {
            synchronized (this.callback) {
                this.callback = callback;
            }
        }
    }

    /**
     * This method is called when the connection to the server is lost
     *
     * @param cause the reason behind the loss of connection
     */
    @Override
    public void connectionLost(Throwable cause) {
        isConnected.set(false);

        logger.info("Broker Connection lost! Trying to reconnect...");
        connect(null, null, null);
    }

    /**
     * This method is called when a message arrives from the server
     * <p>
     * This method is invoked synchronously by the MQTT client. An acknowledgment
     * is not sent back to the server until this method returns cleanly.
     * <p>
     * If an implementation of this method throws an Exception, then the client
     * will be shut down. When the client is next re-connected, any QoS 1 or 2
     * messages will be redelivered by the server.
     * <p>
     * Any additional messages which arrive while an implementation of this method
     * is running, will build up in memory, and will then back up on the network.
     * <p>
     * If an application needs to persist data, then it should ensure the data is
     * persisted prior to returning from this method, as after returning from this
     * method, the message is considered to have been delivered, and will not be
     * reproducible.
     * <p>
     * It is possible to send a new message within an implementation of this callback
     * (for example, a response to this message), but the implementation must not
     * disconnect the client, as it will be impossible to send an acknowledgment
     * for the message being processed, and a deadlock will occur.
     *
     * @param topic   name of the topicHandler on the message was published to
     * @param message the actual message
     * @throws Exception if a terminal error has occurred, and the client should
     *                   be shut down
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (!message.isDuplicate()) {
            synchronized (callback) {
                if (callback != null) {
                    if (encryptMessage) {
                        callback.handleMessage(topic, decryptMessage(message.getPayload()));
                    } else {
                        callback.handleMessage(topic, message.getPayload());
                    }
                }
            }
        }
    }

    /**
     * Called when delivery for a message has been completed, and all acknowledgments
     * have been received. For QoS 0 messages it is called once the message has been
     * handed to the network for delivery. For QoS 1 it is called when PUBACK is
     * received and for QoS 2 when PUBCOMP is received. The token will be the same
     * token as that returned when the message was published.
     *
     * @param token the delivery token associated with the message
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // ignore for now
    }

    /**
     * Add information about device connected to the dongle
     *
     * @param info device information
     */
    @Override
    public void addDevice(DeviceInfo info) {
        this.info.addDevice(info);
    }

    /**
     * Remove device from dongle
     *
     * @param deviceId device ID as a string
     */
    @Override
    public void removeDevice(String deviceId) {
        this.info.removeDevice(deviceId);
    }

    /**
     * Encrypt message before publishing to MQTT broker
     *
     * @param message plaintext message to be encrypted as a byte array
     * @return cipher text as a byte array
     */
    private byte[] encryptMessage(byte[] message) {
        try {
            // Prepare environment
            byte[] iv = new byte[IV_LEN];
            (new SecureRandom()).nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec params = new GCMParameterSpec(TAG_LEN * Byte.SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, params);

            // Perform encryption
            byte[] ciphertext = cipher.doFinal(message);
            byte[] encrypted = new byte[IV_LEN + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, IV_LEN);
            System.arraycopy(ciphertext, 0, encrypted, IV_LEN, ciphertext.length);

            return encrypted;
        } catch (Exception ex) {
            logger.error("Failed to encrypt message!", ex);
            throw new IllegalArgumentException("Failed to encrypt message - " + ex.getMessage());
        }
    }

    /**
     * Decrypt message from MQTT broker (the message was encrypted by the publisher and
     * should be confidential with the broker)
     *
     * @param message cipher text to be decrypted as a byte array
     * @return plaintext as a byte array
     */
    private byte[] decryptMessage(byte[] message) {
        try {
            // Prepare environment
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec params = new GCMParameterSpec(TAG_LEN * Byte.SIZE, message, 0, IV_LEN);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, params);

            return cipher.doFinal(message, IV_LEN, message.length - IV_LEN);
        } catch (Exception ex) {
            logger.error("Failed to decrypt message!", ex);
            throw new IllegalArgumentException("Failed to decrypt message - " + ex.getMessage());
        }
    }

    /**
     * A separate thread to periodically report status to Map Manager
     */
    private class MapManagerPinger extends Thread {
        private final Logger logger = LogManager.getLogger(MapManagerPinger.class);

        private final Dongle dongle;
        private final Gson gson;
        private final String topic;
        private final int interval;

        private MapManagerPinger(Dongle dongle) {
            this.dongle = dongle;
            this.gson = new Gson();
            this.topic = dongle.topicHandler.getStatusTopic(dongle.projectName, dongleId);
            this.interval = dongle.reportInterval;
        }

        @Override
        public void run() {
            String message;
            HashMap<String, Object> options = new HashMap<>();
            options.put("disable_aes", "true");
            while (dongle.isRunning.get()) {
                try {
                    Thread.sleep(interval * 1000);
                    // Generate JSON message
                    synchronized (dongle.info) {
                        message = gson.toJson(dongle.info);
                    }

                    // Publish to Map Manager
                    if (dongle.isConnected()) {
                        if (logger.isDebugEnabled()) logger.debug("Pinging Map Manager...");
                        dongle.publish(topic, message, options);
                    }
                } catch (Exception ex) {
                    // ignore all exceptions
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to ping Map Manager - " + ex.getMessage());
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Pinger is exiting...");
            }
        }
    }
}
