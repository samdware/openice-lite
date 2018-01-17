package edu.upenn.cis.precise.openicelite.middleware.mqtt.type;

import edu.upenn.cis.precise.openicelite.middleware.core.ITopicHandler;

/**
 * Shared class to ensure standardized and centralized place for MQTT topic
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public class TopicHandler implements ITopicHandler {
    private static final String TOPIC_BASE = "PRECISE/OpenICElite/";
    private static final String TOPIC_ONLINE = "Online";
    private static final String TOPIC_CLIENT = "Client";
    private static final String TOPIC_STATUS = "Status";
    private static final String TOPIC_DATA = "Data";

    public TopicHandler() {

    }

    /**
     * Return "Online" topic managed by MapManager based on project name
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    public String getOnlineTopic(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return null;
        }
        return TOPIC_BASE + projectName + "/" + TOPIC_ONLINE;
    }

    /**
     * Return root topic for all clients (should be used by MapManager to subscribe to
     * all client topics)
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    public String getClientBaseTopic(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return null;
        }
        return TOPIC_BASE + projectName + "/" + TOPIC_CLIENT + "/#";
    }

    /**
     * Return topic for corresponding client
     *
     * @param projectName project name
     * @param clientName client name
     * @return corresponding topic name
     */
    public String getClientTopic(String projectName, String clientName) {
        if (projectName == null || projectName.isEmpty()
                || clientName == null || clientName.isEmpty()) {
            return null;
        }
        return TOPIC_BASE + projectName + "/" + TOPIC_CLIENT + "/" + clientName;
    }

    /**
     * Return root topic for all statues (should be used by MapManager to subscribe to
     * all status topics)
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    public String getStatusBaseTopic(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return null;
        }
        return TOPIC_BASE + projectName + "/" + TOPIC_STATUS + "/#";
    }

    /**
     * Return status topic based on project name, dongle ID
     *
     * @param projectName project name
     * @param dongleId    dongle ID
     * @return corresponding topic name
     */
    @Override
    public String getStatusTopic(String projectName, String dongleId) {
        if (projectName == null || projectName.isEmpty()
                || dongleId == null || dongleId.isEmpty()) {
            return null;
        }

        return TOPIC_BASE + projectName + "/" + TOPIC_STATUS + "/" + dongleId;
    }

    /**
     * Return root topic for all data
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    @Override
    public String getDataBaseTopic(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return null;
        }
        return TOPIC_BASE + projectName + "/" + TOPIC_DATA + "/#";
    }

    /**
     * Return data topic based on project name, dongle ID, and device ID
     *
     * @param projectName project name
     * @param dongleId dongle ID
     * @param deviceId device ID
     * @return corresponding topic name
     */
    public String getDataTopic(String projectName, String dongleId, String deviceId) {
        if (projectName == null || projectName.isEmpty()
                || dongleId == null || dongleId.isEmpty()
                || deviceId == null || deviceId.isEmpty()) {
            return null;
        }

        return TOPIC_BASE + projectName + "/" + TOPIC_DATA + "/" + dongleId + "/" + deviceId;
    }

    /**
     * Return topic type from published topic
     *
     * @param topic topic name
     * @return corresponding topic type
     */
    @Override
    public TopicType getTopicType(String topic) {
        if (topic == null || topic.isEmpty()) return null;

        String[] tokens = topic.replace(TOPIC_BASE, "").split("/");
        if (tokens.length < 4) return null;
        switch (tokens[1]) {
            case "Data":
                return TopicType.DATA;
            case "Status":
                return TopicType.STATUS;
            case "Client":
                return TopicType.CLIENT;
            default:
                return null;
        }
    }

    /**
     * Return project name, dongle ID, device ID as a String array with the same
     * order from published topic
     *
     * @param topic topic name
     * @return a String array as [project name, dongle ID, device ID]
     */
    @Override
    public String[] getTopicInfo(String topic) {
        if (topic == null || topic.isEmpty()) return null;

        String[] tokens = topic.replace(TOPIC_BASE, "").split("/");
        if (tokens.length < 4) return null;

        String[] info = new String[3];
        info[0] = tokens[0];
        info[1] = tokens[2];
        info[2] = tokens[3];
        return info;
    }

    /**
     * Return project name from published topic
     *
     * @param topic topic name
     * @return corresponding project name
     */
    @Override
    public String getProjectName(String topic) {
        if (topic == null || topic.isEmpty()) return null;

        String[] tokens = topic.replace(TOPIC_BASE, "").split("/");
        if (tokens.length < 4) return null;
        return tokens[0];
    }

    /**
     * Return dongle ID from published topic
     *
     * @param topic topic name
     * @return corresponding dongle ID
     */
    @Override
    public String getDongleId(String topic) {
        if (topic == null || topic.isEmpty()) return null;

        String[] tokens = topic.replace(TOPIC_BASE, "").split("/");
        if (tokens.length < 4) return null;
        return tokens[2];
    }

    /**
     * Return device ID from published topic
     *
     * @param topic topic name
     * @return corresponding device ID
     */
    @Override
    public String getDeviceId(String topic) {
        if (topic == null || topic.isEmpty()) return null;

        String[] tokens = topic.replace(TOPIC_BASE, "").split("/");
        if (tokens.length < 4) return null;
        return tokens[3];
    }
}
