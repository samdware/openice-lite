package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

/**
 * A class that holds information about a connection
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public class ConnectionInfo extends Info{

	public static final String[] required = {
		"name", "channels", "recv_oct_details_rate", "send_oct_details_rate", "ssl"	
	};
	
	public ConnectionInfo() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String[] requiredFields() {
		return required;
	}
	
	

}
