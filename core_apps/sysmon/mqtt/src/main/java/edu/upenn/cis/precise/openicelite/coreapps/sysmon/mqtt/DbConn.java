package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DbConn {
    private static Logger logger = LogManager.getLogger(DbConn.class);

    private Connection conn = null;
    private DatabaseMetaData meta = null;

    protected void connect(String fileName) {
        try {
            String url = "jdbc:sqlite:" + fileName;
            conn = DriverManager.getConnection(url);
            logger.info("Connection to db established: {}]", fileName);
            meta = conn.getMetaData();
        } catch (SQLException e) {
            logger.error("Error connecting to database ", e);
        }
    }

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
     * @param name Name of the table
     */
    protected void createConnectionTable(String name) {
        String sql = "CREATE TABLE IF NOT EXISTS " + name + " (\n"
                + " id integer PRIMARY KEY, \n"
                + " time text NOT NULL,\n"
                + " "
                + ");";
        try {
            Statement st = conn.createStatement();
            st.execute(sql);
        } catch (SQLException e) {
            logger.error("Error while creating table ", e);
        }

    }
}
