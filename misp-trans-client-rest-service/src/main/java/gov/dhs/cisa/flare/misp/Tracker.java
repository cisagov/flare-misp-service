package gov.dhs.cisa.flare.misp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tracker implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@JsonProperty("stix_id")
	String stixId;

	@JsonProperty("event_id")
	String eventId;

	@JsonProperty("status_code")
	int status_code;

	public int getStatusCode() {
		return status_code;
	}

	public void setStatusCode(int status_code) {
		this.status_code = status_code;
	}

	public String getStixId() {
		return stixId;
	}

	public void setStixId(String id) {
		this.stixId = id;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eid) {
		this.eventId = eid;
	}
	
}