package edu.upenn.cis.precise.openicelite.middleware.mqtt;

import edu.upenn.cis.precise.openicelite.middleware.core.IGracefulShutdown;
import edu.upenn.cis.precise.openicelite.middleware.core.ShutdownHook;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.type.DongleInfo;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.type.TopicHandler;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.util.SSLUtil;

import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Map Manager implementation to keep track of online OpenICE-lite dongles and available
 * data topics
 * <p>
 * This class implements MqttCallback to handle MQTT related events and ensure fault-tolerance
 * in case of disconnection happens. Map Manager requires a properties file to be loaded for
 * configuration.
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
 * - "ca_cert_file":        absolute path to CA certification for TLS connection
 * - "client_cert_file":    absolute path to client certification for TLS connection
 *                              (if broker requires client cert)
 * - "client_key_file":     absolute path to client private key for TLS connection
 * - "key_password":        password to unlock client private key if needed
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class MapManager implements MqttCallback, IGracefulShutdown {
    private static final String PROPERTIES_FILE_NAME = "map_manager.properties";

    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final String MQTT_NAME = "MapManager-" + UUID;

    private static final Logger logger = LogManager.getLogger(MapManager.class);

    // Overwritten-able configuration (using exact same property with all lower case
    // and separated with underscore)
    // -- MQTT broker address (main and fail-over)
    private String broker = "tcp://iot.eclipse.org:1883";
    private String[] brokers = null;
    // -- broker connection timeout (seconds)
    private int connectionTimeout = 60;
    // -- broker connection retry interval (seconds)
    private int retryInterval = 15;
    // -- broker keep alive interval (seconds)
    private int aliveInterval = 60;
    // -- QoS setting
    private int qos = 1;
    // -- project name (can be used to separate environment for different projects
    private String projectName = "DEFAULT";
    // -- interval to publish online device list to topic (in seconds)
    private int reportInterval = 15;
    // -- dongle must ping within this time-frame to maintain alive status (seconds)
    private int dongleOnlineTimeout = 120;

    // Additional information from properties file
    private boolean useAuthentication = false;
    private String username = null;
    private String password = null;

    private boolean useSSL = false;
    private String caCertFile;
    private String clientCertFile;
    private String clientKeyFile;
    private String keyPassword;

    // MQTT
    private TopicHandler topic = new TopicHandler();
    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isClosing = new AtomicBoolean(false);

    private String statusTopic;
    private String clientTopic;

    // Dongle information
    Gson gson = new Gson();
    private ConcurrentHashMap<String, DongleInfo> dongles = new ConcurrentHashMap<>();
    private Reporter reporter;

    public static void main(String[] args) {
        MapManager manager = new MapManager();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(manager));

        logger.info("Starting Map Manager");
        manager.init();
        manager.connect();
    }

    /**
     * Initialize Map Manager with default configuration or properties file
     * <p>
     * SSL must be enabled via properties file.
     */
    public void init() {
        ClassLoader loader = getClass().getClassLoader();
        URL propResource = loader.getResource(PROPERTIES_FILE_NAME);
        File propFile = new File("./" + PROPERTIES_FILE_NAME);

        if (propFile.isFile() || propResource != null) {
            logger.info("Loading configuration from " +
                    (propFile.isFile() ? "./" : "default ") + PROPERTIES_FILE_NAME + "...");
            try (InputStream input = (propFile.isFile() ?
                    new FileInputStream("./" + PROPERTIES_FILE_NAME) :
                    loader.getResourceAsStream(PROPERTIES_FILE_NAME))) {
                // Load properties file
                Properties properties = new Properties();
                properties.load(input);
                if (properties.containsKey("broker")) {
                    broker = properties.getProperty("broker");
                }
                if (properties.containsKey("brokers")) {
                    brokers = properties.getProperty("brokers").split(",");
                }
                if (properties.containsKey("connection_timeout")) {
                    connectionTimeout = Integer.parseInt(properties.getProperty("connection_timeout"));
                }
                if (properties.containsKey("retry_interval")) {
                    retryInterval = Integer.parseInt(properties.getProperty("retry_interval"));
                }
                if (properties.containsKey("alive_interval")) {
                    aliveInterval = Integer.parseInt(properties.getProperty("alive_interval"));
                }
                if (properties.containsKey("qos")) {
                    qos = Integer.parseInt(properties.getProperty("qos"));
                }
                if (properties.containsKey("project_name")) {
                    projectName = properties.getProperty("project_name");
                }
                if (properties.containsKey("report_interval")) {
                    reportInterval = Integer.parseInt(properties.getProperty("report_interval"));
                }
                if (properties.containsKey("dongle_online_timeout")) {
                    dongleOnlineTimeout = Integer.parseInt(properties.getProperty("dongle_online_timeout"));
                }
                if (properties.containsKey("username") && properties.containsKey("password")) {
                    useAuthentication = true;
                    username = properties.getProperty("username");
                    password = properties.getProperty("password");
                }
                if (properties.containsKey("ca_cert_file")) {
                    useSSL = true;
                    caCertFile = properties.getProperty("ca_cert_file");
                    clientCertFile = properties.getProperty("client_cert_file");
                    clientKeyFile = properties.getProperty("client_key_file");
                    keyPassword = properties.getProperty("key_password");
                }
            } catch (Exception ex) {
                logger.error("Failed to initialize Map Manager", ex);
                ex.printStackTrace();
                System.exit(-1);
            }
        } else {
            logger.info("Cannot find properties file! Using default configuration!");
        }

        // Finalize connect option
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(connectionTimeout);
        mqttConnectOptions.setKeepAliveInterval(aliveInterval);
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
     * Connect to broker using configured options
     * <p>
     * Map Manager never exits and keeps retrying until connected. Retry interval can be configured
     * via properties file.
     */
    public void connect() {
        while (!isConnected.get()) {
            try {
                if (mqttClient == null) {
                    MqttDefaultFilePersistence dataStore =
                            new MqttDefaultFilePersistence("storage/" + projectName);
                    mqttClient = new MqttAsyncClient(broker, MQTT_NAME, dataStore);
                    mqttClient.setCallback(this);
                } else {
                    // Wait for retry if needed
                    Thread.sleep(retryInterval * 1000);
                }

                // Connect to broker
                logger.info("Connecting to broker at " + broker + " as " + MQTT_NAME + " ...");
                IMqttToken connectToken = mqttClient.connect(mqttConnectOptions);
                connectToken.waitForCompletion();
                logger.info("Map Manager is connected to broker!");

                // Subscribe to pre-defined topics
                String topicName = topic.getClientBaseTopic(projectName);
                clientTopic = topicName.replace("#", "");
                logger.info("Subscribing to " + topicName + " ...");
                IMqttToken subscribeToken = mqttClient.subscribe(topicName, qos, null, null);
                subscribeToken.waitForCompletion();
                topicName = topic.getStatusBaseTopic(projectName);
                statusTopic = topicName.replace("#", "");
                logger.info("Subscribing to " + topicName + " ...");
                subscribeToken = mqttClient.subscribe(topicName, qos, null, null);
                subscribeToken.waitForCompletion();
                logger.info("Map Manager is ready!");

                if (reporter == null) {
                    reporter = new Reporter(this, topic.getOnlineTopic(projectName));
                    reporter.start();
                }

                isConnected.set(true);
            } catch (Exception ex) {
                isConnected.set(false);
                logger.error("Failed to connect to MQTT broker", ex);
                logger.info("Retry in " + retryInterval + " seconds ...");
            }
        }
    }

    /**
     * Disconnect from broker
     */
    @Override
    public void disconnect() {
        if (isConnected.get()) {
            try {
                logger.info("Disconnecting from broker ...");
                IMqttToken disconnectToken = mqttClient.disconnect(null, null);
                disconnectToken.waitForCompletion(1000);
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
        isClosing.set(true);
    }

    /**
     * This method is called when the connection to the server is lost
     *
     * @param cause the reason behind the loss of connection
     */
    @Override
    public void connectionLost(Throwable cause) {
        isConnected.set(false);

        logger.info("Broker Connection lost! Trying to reconnect ...");
        connect();
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
     * @param topic   name of the topic on the message was published to
     * @param message the actual message
     * @throws Exception if a terminal error has occurred, and the client should
     *                   be shut down
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        long now = System.currentTimeMillis();
        try {
            if (topic.contains(statusTopic)) {
                // Dongle updates status
                DongleInfo info = gson.fromJson(new String(message.getPayload()), DongleInfo.class);
                info.setLastUpdated(now);
                dongles.put(info.getDongleId(), info);
            }
        } catch (Exception ex) {
            logger.error("Failed to process incoming message!", ex);
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
     * A separate thread to periodically report online devices to Online topic
     */
    private class Reporter extends Thread {
        private final MapManager manager;
        private final String topic;
        private final Gson gson = new Gson();

        public Reporter(MapManager manager, String topic) {
            this.topic = topic;
            this.manager = manager;
        }

        @Override
        public void run() {
            while (!manager.isClosing.get()) {
                try {
                    Thread.sleep(manager.reportInterval * 1000);

                    // Check for online devices
                    long now = System.currentTimeMillis();
                    LinkedList<DongleInfo> dongles = new LinkedList<>();
                    for (DongleInfo info : manager.dongles.values()) {
                        if (now - info.getLastUpdated() < manager.dongleOnlineTimeout * 1000) {
                            dongles.add(info);
                        }
                    }

                    // Generate message and publish
                    if (manager.isConnected.get()) {
                        String message = gson.toJson(dongles);
                        manager.mqttClient.publish(topic, message.getBytes(), 1, false);
                    }
                } catch (Exception ex) {
                    logger.error("Failed to report online devices!");
                }
            }
        }
    }
}
