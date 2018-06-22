package edu.upenn.cis.precise.openicelite.middleware.api;

/**
 * Shared class to ensure standardized and centralized place for topic names
 *
 * @author Hung Nguyen (hungng@seas.upenn.edu)
 */
public interface ITopicHandler {
    /**
     * Return "Online" topic managed by MapManager based on project name
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    String getOnlineTopic(String projectName);

    /**
     * Return root topic for all clients (should be used by MapManager to subscribe to
     * all client topics)
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    String getClientBaseTopic(String projectName);

    /**
     * Return topic for corresponding client
     *
     * @param projectName project name
     * @param clientName  client name
     * @return corresponding topic name
     */
    String getClientTopic(String projectName, String clientName);

    /**
     * Return root topic for all statues (should be used by MapManager to subscribe to
     * all status topics)
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    String getStatusBaseTopic(String projectName);

    /**
     * Return status topic based on project name, dongle ID
     *
     * @param projectName project name
     * @param dongleId    dongle ID
     * @return corresponding topic name
     */
    String getStatusTopic(String projectName, String dongleId);

    /**
     * Return root topic for all data
     *
     * @param projectName project name
     * @return corresponding topic name
     */
    String getDataBaseTopic(String projectName);

    /**
     * Return data topic based on project name, dongle ID, and device ID
     *
     * @param projectName project name
     * @param dongleId    dongle ID
     * @param deviceId    device ID
     * @return corresponding topic name
     */
    String getDataTopic(String projectName, String dongleId, String deviceId);

    /**
     * Return topic type from published topic
     *
     * @param topic topic name
     * @return corresponding topic type
     */
    TopicType getTopicType(String topic);

    /**
     * Return project name, dongle ID, device ID as a String array with the same
     * order from published topic
     *
     * @param topic topic name
     * @return a String array as [project name, dongle ID, device ID]
     */
    String[] getTopicInfo(String topic);

    /**
     * Return project name from published topic
     *
     * @param topic topic name
     * @return corresponding project name
     */
    String getProjectName(String topic);

    /**
     * Return dongle ID from published topic
     *
     * @param topic topic name
     * @return corresponding dongle ID
     */
    String getDongleId(String topic);

    /**
     * Return device ID from published topic
     *
     * @param topic topic name
     * @return corresponding device ID
     */
    String getDeviceId(String topic);

    public enum TopicType {
        DATA, STATUS, CLIENT
    }
}
