package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ChannelInfo;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ConnectionInfo;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.Info;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.List;

public class DbConn {
    private static Logger logger = LogManager.getLogger(DbConn.class);

    private Connection conn;
    private String url;

    protected DbConn(String fileName) {
        this.url = "jdbc:sqlite:" + fileName;
    }

    /**
     * Connects to the database file
     */
    protected void connect() {
        try {
            conn = DriverManager.getConnection(url);
            logger.info("Connection to db established: {}]", url);
        } catch (SQLException e) {
            logger.error("Error connecting to database ", e);
        }
    }

    /**
     * Disconnects from the database file.
     */
    protected void disconnect() {
        try {
            if (conn != null) {
                conn.close();
                logger.info("Connection to database closed");
            }
        } catch (SQLException e) {
            logger.error("Error while disconnecting from database ", e);
        }
    }

    /**
     * Creates (if not exists) a table that can store ConnectionInfo
     */
    protected void createConnectionTable() {
        String sql = "CREATE TABLE IF NOT EXISTS connections (\n"
                + " id integer PRIMARY KEY, \n"
                + " time text NOT NULL,\n"
                + " name text, \n"
                + " username text, \n"
                + " state text, \n"
                + " ssl text, \n"
                + " channels integer, \n"
                + " recv_oct_rate real, \n"
                + " recv_oct_count integer, \n"
                + " send_oct_rate real, \n"
                + " send_oct_count integer \n"
                + ");";
        try {
            Statement st = conn.createStatement();
            st.execute(sql);
        } catch (SQLException e) {
            logger.error("Error while creating table connections", e);
        }
    }

    /**
     * Inserts the given ConnectionInfo into connections table.
     * @param info ConnectionInfo object to insert
     */
    protected void insertConnectionInfo(ConnectionInfo info) {
       String sql = "INSERT INTO connections(time, name, username, state, ssl, channels,"
               + "recv_oct_rate, recv_oct_count, send_oct_rate, send_oct_count)"
               + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
       try {
           PreparedStatement pst = conn.prepareStatement(sql);
           pst.setString(1, info.getAsString("time"));
           pst.setString(2, info.getAsString("name"));
           pst.setString(3, info.getAsString("username"));
           pst.setString(4, info.getAsString("state"));
           pst.setString(5, info.getAsString("ssl"));
           pst.setLong  (6, info.getAsLong("channels"));
           pst.setDouble(7, info.getAsDouble("recv_oct_rate"));
           pst.setLong  (8, info.getAsLong("recv_oct_count"));
           pst.setDouble(9, info.getAsDouble("send_oct_rate"));
           pst.setLong  (10, info.getAsLong("send_oct_count"));
           pst.execute();
       } catch (SQLException e) {
           logger.error("Error while inserting to table connections", e);
       }
    }

    /**
     * Convenience method to insert a list of ConnectionInfo
     * @param infoList List of ConnectionInfo to insert
     */
    protected void insertConnectionList(List<Info> infoList) {
        for (Info info : infoList) {
            insertConnectionInfo((ConnectionInfo) info);
        }
    }

    /**
     * Creates (if not exists) a table that can store ChannelInfo
     */
    protected void createChannelTable() {
        String sql = "CREATE TABLE IF NOT EXISTS channels (\n"
                + " id integer PRIMARY KEY, \n"
                + " time text NOT NULL, \n"
                + " name text, \n"
                + " username text, \n"
                + " connection text, \n"
                + " state text, \n"
                + " publish_rate real, \n"  // publisher metrics
                + " publish_count integer, \n"
                + " messages_unacknowledged integer, \n"
                + " deliver_get_rate real, \n"  // subscriber metrics
                + " deliver_get_count integer, \n"
                + " ack_rate real, \n"
                + " ack_count integer, \n"
                + " redeliver_rate real, \n"
                + " redeliver_count integer \n"
                + ");";
        try {
            Statement st = conn.createStatement();
            st.execute(sql);
        } catch (SQLException e) {
            logger.error("Error while creating table", e);
        }
    }

    /**
     * Inserts the given ChannelInfo object into table
     * @param info ChannelInfo to insert
     */
    protected void insertChannelInfo(ChannelInfo info) {
        String sql = "INSERT INTO channels (time, name, username, connection, state,"
                + " publish_rate, publish_count, messages_unacknowledged,"
                + " deliver_get_rate, deliver_get_count, ack_rate, ack_count, redeliver_rate, redeliver_count)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, info.getAsString("time"));
            pst.setString(2, info.getAsString("name"));
            pst.setString(3, info.getAsString("username"));
            pst.setString(4, info.getAsString("connection"));
            pst.setString(5, info.getAsString("state"));
            pst.setDouble(6, info.getAsDouble("publish_rate"));
            pst.setLong  (7, info.getAsLong("publish_count"));
            pst.setLong  (8, info.getAsLong("messages_unacknowledged"));
            pst.setDouble(9, info.getAsDouble("deliver_get_rate"));
            pst.setLong  (10, info.getAsLong("deliver_get_count"));
            pst.setDouble(11, info.getAsDouble("ack_rate"));
            pst.setLong  (12, info.getAsLong("ack_count"));
            pst.setDouble(13, info.getAsDouble("redeliver_rate"));
            pst.setLong  (14, info.getAsLong("redeliver_count"));
            pst.execute();
        } catch (SQLException e) {
            logger.error("Error while inserting to table channels", e);
        }
    }

    /**
     * Convenience method to insert a list of ChanneInfo
     * @param infoList List of ChannelInfo to insert
     */
    protected void insertChannelList(List<Info> infoList) {
        for (Info info : infoList) {
            insertChannelInfo((ChannelInfo) info);
        }
    }

    /**
     * Convenience methods to insert a list of Info
     * @param m Name of metric that the list holds
     * @param infoList List to insert
     */
    protected void insertList(String m, List<Info> infoList) {
        if ("connections".equals(m)) {
            insertConnectionList(infoList);
        } else if ("channels".equals(m)) {
            insertChannelList(infoList);
        }
    }
}
