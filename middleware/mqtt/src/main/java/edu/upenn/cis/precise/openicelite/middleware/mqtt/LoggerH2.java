package edu.upenn.cis.precise.openicelite.middleware.mqtt;

import edu.upenn.cis.precise.openicelite.middleware.api.*;
import edu.upenn.cis.precise.openicelite.middleware.mqtt.type.TopicHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.DeleteDbFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * A simple logger application to collect data published by MQTT Dongle via MQTT
 * brokers and save to embedded H2 database.
 * <p>
 * Available options for initialization:
 * - "db_user":             (required) username to access H2 database file
 * - "db_pass":             (required) password to access H2 database file
 * - "db_encryption_pass":  (required) encryption password to access H2 database file
 * - "project_name":        project name will be used as a base topic separation
 *                              and database file name (default to DEFAULT)
 * - "dongle_id":           dongle UUID to identify dongle and used as part of data topic
 *                              (will be randomized if not provided)
 * - "device_id":           device UUID to identify device connected to dongle
 *                              and used as part of data topic (will be randomized if not provided)
 * - "db_dir":              relative path to directory to storage database file
 * - "broker":              full address to the main broker
 *                              (e.g., ssl://hostname:port)
 * - "brokers":             fail-over addresses, separated with comma
 * - "connection_timeout":  timeout for connecting to broker
 * - "retry_interval":      retry interval if lost connection
 * - "alive_interval":      alive interval for MQTT connection
 * - "qos":                 MQTT QoS settings (0,1,2)
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
public class LoggerH2 implements IGracefulShutdown {
    private static final String PROPERTIES_FILE_NAME = "logger_h2.properties";
    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final String DB_CONNECTION = "jdbc:h2:";

    private static final Logger logger = LogManager.getLogger(LoggerH2.class);

    private final String clientName;
    private final String dbDir;
    private final String dbName;
    private final String dbUser;
    private final String dbPass;
    private final String dbEncryptionPass;
    private final HashMap<String, Object> options;

    // Running objects
    private Connection dbConnection;
    private IMiddleware middleware;
    private ITopicHandler topicHandler;

    public static void main(String[] args) {
        logger.info("Starting H2 Logger...");

        // Load configuration
        ClassLoader loader = Dongle.class.getClassLoader();
        URL propResource = loader.getResource(PROPERTIES_FILE_NAME);
        File propFile = new File("./" + PROPERTIES_FILE_NAME);

        String dongleId = UUID;
        String clientName = "Logger-";
        String dbDir = "data";
        String dbName = "DEFAULT";
        String dbUser = null;
        String dbPass = null;
        String dbEncryptionPass = null;
        HashMap<String, Object> options = new HashMap<>();

        if (propFile.isFile() || propResource != null) {
            logger.info("Loading configuration from " +
                    (propFile.isFile() ? "./" : "default ") + PROPERTIES_FILE_NAME + "...");
            try (InputStream input = (propFile.isFile() ?
                    new FileInputStream("./" + PROPERTIES_FILE_NAME) :
                    loader.getResourceAsStream(PROPERTIES_FILE_NAME))) {
                Properties properties = new Properties();
                properties.load(input);

                // -- dongle ID
                if (properties.containsKey("dongle_id")) {
                    dongleId = properties.getProperty("dongle_id");
                }
                clientName += dongleId;
                logger.info("-- Dongle ID     : " + dongleId);
                logger.info("-- Client Name   : " + clientName);
                // -- database location
                if (properties.containsKey("db_dir")) {
                    dbDir = properties.getProperty("db_dir");
                }
                if (properties.containsKey("project_name")) {
                    dbName = properties.getProperty("project_name");
                }
                dbDir = Paths.get(dbDir).toAbsolutePath().toString();
                logger.info("-- Data Path     : " + dbDir + "/" + dbName);
                // -- username
                dbUser = properties.getProperty("db_user");
                // -- password
                dbPass = properties.getProperty("db_pass");
                // -- encryption password
                dbEncryptionPass = properties.getProperty("db_encryption_pass");

                if (dbUser == null || dbPass == null || dbEncryptionPass == null) {
                    throw new IllegalArgumentException("Invalid properties file!");
                }

                // -- other properties
                Enumeration<?> p = properties.propertyNames();
                while (p.hasMoreElements()) {
                    String key = (String) p.nextElement();
                    String value = properties.getProperty(key);
                    options.put(key, value);
                }
            } catch (Exception ex) {
                logger.error("Failed to load configuration!", ex);
                System.exit(-1);
            }
        } else {
            logger.error("Cannot find configuration file - " + PROPERTIES_FILE_NAME + "!");
            System.exit(-1);
        }

        // Initialize database
        LoggerH2 log = new LoggerH2(clientName, dbDir, dbName, dbUser, dbPass, dbEncryptionPass, options);
        log.init(false);
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(log));

        // Start collecting data
        log.startMiddleware();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(log.middleware));
        log.start();
    }

    /**
     * Logger constructor
     *
     * @param clientName       MQTT Client name
     * @param dbDir            absolute path to database directory
     * @param dbName           database file name
     * @param dbUser           database username
     * @param dbPass           database password
     * @param dbEncryptionPass database encryption password
     * @param options          additional options to initialize MQTT dongle
     */
    public LoggerH2(String clientName,
                    String dbDir, String dbName, String dbUser, String dbPass, String dbEncryptionPass,
                    HashMap<String, Object> options) {
        this.clientName = clientName;
        this.dbDir = dbDir;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.dbEncryptionPass = dbEncryptionPass;
        this.options = options;
    }

    /**
     * Initialize H2 database
     * <p>
     * Any exception will cause the application to exit without any retry
     *
     * @param clean indicate if needed to delete exist data file
     */
    public void init(boolean clean) {
        try {
            // Clean-up if needed
            if (dbConnection != null && !dbConnection.isClosed()) dbConnection.close();
            if (clean) DeleteDbFiles.execute(dbDir, dbName, true);

            // Initiate connection
            Class.forName("org.h2.Driver");
            dbConnection = DriverManager.getConnection(DB_CONNECTION + dbDir + "/" + dbName +
                            ";MV_STORE=FALSE;MVCC=FALSE;CIPHER=AES",
                    dbUser, dbEncryptionPass + " " + dbPass);

            // Create table if it doesn't exist
            prepareTable(dbConnection, dbName);
        } catch (Exception ex) {
            logger.error("Failed to initialize database!", ex);
            System.exit(-1);
        }
    }

    /**
     * Initialize middleware object, then connect to MQTT broker
     */
    public void startMiddleware() {
        middleware = new Dongle(clientName);
        middleware.init(options);
        middleware.connect(null, null, null);

        topicHandler = new TopicHandler();
    }

    /**
     * Start logging data
     */
    public void start() {
        middleware.waitConnected();
        MiddlewareCallback callback = new MiddlewareCallback(this);
        middleware.subscribe(topicHandler.getDataBaseTopic(dbName), null, callback);
    }

    /**
     * Stop logging data
     * <p>
     * TODO: implement this
     */
    public void stop() {

    }

    /**
     * Disconnect from broker
     */
    @Override
    public void disconnect() {
        // Does not apply in this context
    }

    /**
     * Close and release all associated resource
     */
    @Override
    public void close() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                logger.info("Closing database...");
                dbConnection.close();
            }
        } catch (Exception ex) {
            logger.warn("Failed to gracefully shutdown!", ex);
        }
    }

    private class MiddlewareCallback implements IMiddlewareCallback {
        private final LoggerH2 log;
        private PreparedStatement pStat = null;

        MiddlewareCallback(LoggerH2 log) {
            this.log = log;
        }

        /**
         * Handle new message from middleware as a byte array
         *
         * @param topic   the topic the message was delivered from
         * @param message the payload as a byte array
         */
        @Override
        public void handleMessage(String topic, byte[] message) {
            try {
                if (pStat == null) pStat = log.prepareStatement(log.dbConnection, log.dbName);

                if (topicHandler.getTopicType(topic) == ITopicHandler.TopicType.DATA) {
                    // Populate query
                    String[] tokens = log.topicHandler.getTopicInfo(topic);
                    pStat.setString(1, tokens[1]);
                    pStat.setString(2, tokens[2]);
                    pStat.setString(3, new String(message));
                    pStat.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    // Execute
                    pStat.execute();
                }
            } catch (Exception ex) {
                logger.error("Failed to handle incoming message!", ex);
            }
        }

        /**
         * Handle new message from middleware as a string
         *
         * @param topic   the topic the message was delivered from
         * @param message the payload as a string
         */
        @Override
        public void handleMessage(String topic, String message) {

        }
    }

    // ** H2 SQL Statements ** //

    /**
     * Create data table with predefined schema if not exists
     *
     * @param connection opened connection to database
     * @param tableName  table name
     */
    private static void prepareTable(Connection connection, String tableName) throws SQLException {
        Statement stat = connection.createStatement();
        String query = "CREATE TABLE IF NOT EXISTS `" + tableName + "` " +
                "(`ID` BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "`DongleID` VARCHAR(50) NOT NULL," +
                "`DeviceID` VARCHAR(50) NOT NULL," +
                "`Message` TEXT NOT NULL," +
                "`ReceivedTime` TIMESTAMP NOT NULL);";
        stat.execute(query);
        stat.close();
    }

    private PreparedStatement prepareStatement(Connection connection, String tableName)
            throws SQLException {
        String query = "INSERT INTO `" + tableName + "` " +
                "(`DongleID`, `DeviceID`, `Message`, `ReceivedTime`) VALUES " +
                "(?,?,?,?);";
        return connection.prepareStatement(query);
    }
}
