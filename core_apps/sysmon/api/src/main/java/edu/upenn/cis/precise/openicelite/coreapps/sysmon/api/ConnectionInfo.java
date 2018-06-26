package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * A class that holds information about a connection
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public class ConnectionInfo extends Info{

	public static final String[] required = {
			"time", "name", "username", "state", "ssl", "channels",
           "recv_rate", "recv_count", "send_rate", "send_count"
	};
	
	public ConnectionInfo() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String[] requiredFields() {
		return required;
	}
	
	

}
