package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * A class that holds information about a connection
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public class ConnectionInfo extends Info{

	public static final String[] required = {
			"time", "name", "username", "state", "ssl", "channels",
           "recv_oct_rate", "recv_oct_count", "send_oct_rate", "send_oct_count"
	};

	@Override
	public String[] requiredFields() {
		return required;
	}
	
	

}
