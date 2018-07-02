package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class the represents data from the system monitor
 *
 * @author Hyun Jung (hyju@seas.upenn.edu)
 */
public class Info {
	private HashMap<String, Object> content;
	
	public Info() {
		content = new HashMap<>();
		guarantee();
	}

    /**
     * Populates the required fields with empty strings, making sure that the required fields exist
     */
	private void guarantee() {
		for (String s : requiredFields()) {
			content.put(s, "");
		}
	}

    /**
     * Returns a list of fields guaranteed to exist for this data.
     * @return String array of the names of the guaranteed fields
     */
	public String[] requiredFields() {
	    return new String[0];
    }

    /**
     * Returns whether this Info contains the specified field.
     * @param field Name of the field to look for
     * @return True if this info contains the field; false otherwise.
     */
    public boolean containsField(String field) {
	    return content.containsKey(field);
    }

    /**
     * Adds a field and a value to the instance. Both the field and the value cannot be null.
     * @param field Name of the field to add
     * @param value Value of the field
     */
	public void add(String field, Object value) {
	    if (field == null || value == null) {
	        throw new NullPointerException("Field and value cannot be null");
        }
		content.put(field, value);
	}

    /**
     * Returns the value of the field given as a String, or an empty string if the field doesn't exist.
     * @param field Name of the field to find
     * @return Value of the field as a string, or null if the field doesn't exist
     */
	public String getAsString(String field) {
		if (content.containsKey(field)) {
			return content.get(field).toString();
		}
		return "";
	}

    /**
     * Returns the value of the field given as a long, or 0 if the field doesn't exist.
     * @param field Name of the field to find
     * @return value of the field as a Long, or null if the field doesn't exist
     */
	public long getAsLong(String field) {
		if (content.containsKey(field) && (content.get(field) instanceof Long)) {
			return (Long) content.get(field);
		}
		return 0;
	}

    /**
     * Returns the value of the field given as a double, or 0 if the field doesn't exist.
     * @param field Name of the field to find
     * @return value of the field as a double, or null if the field doesn't exist
     */
	public double getAsDouble(String field) {
		if (content.containsKey(field) && (content.get(field) instanceof Double)) {
			return (Double) content.get(field);
		}
		return 0;
	}

    /**
     * Returns all data in this Info as a map.
     * @return Map representation of all data
     */
	public Map<String, Object> getMap() {
	    return Collections.unmodifiableMap(content);
    }
 
}
