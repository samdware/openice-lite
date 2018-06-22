package edu.upenn.cis.precise.openicelite.coreapps.sysmon.api;

import java.util.HashMap;

public class Info {
	protected HashMap<String, Object> content;
	
	public Info() {
		content = new HashMap<>();
		guarantee();
	}
	
	private void guarantee() {
		for (String s : requiredFields()) {
			content.put(s, "");
		}
	}
	
	public String[] requiredFields() {
	    return new String[0];
    }
	
	public void add(String field, Object object) {
		content.put(field, object);
	}
	
	public String getAsString(String name) {
		if (content.containsKey(name)) {
			return content.get(name).toString();
		}
		return null;
	}
	
	public Long getAsLong(String name) {
		if (content.containsKey(name) && (content.get(name) instanceof Long)) {
			return (Long) content.get(name);
		}
		return null;
	}
	
	public Double getAsDouble(String name) {
		if (content.containsKey(name) && (content.get(name) instanceof Double)) {
			return (Double) content.get(name);
		}
		return null;
	}
 
}
