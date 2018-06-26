package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * A class that holds information about a channel
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public class ChannelInfo extends Info {
	
	public static final String[] required = {
			"time", "name", "username", "connection", "state", "publish_rate", "publish_count", "messages_unacknowledged",
            "deliver_get_rate", "deliver_get_count", "ack_rate", "ack_count", "redeliver_rate", "redeliver_count"
	};

	public ChannelInfo() {
		super();
	}

	@Override
    public String[] requiredFields() {
	    return required;
    }
}
