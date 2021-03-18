package gov.dhs.cisa.flare.misp;

public class HealthCheckResponse {

	private final String resourceType;
	private final String resource;
	private final int statusCode;

	public HealthCheckResponse(String resourceType, String resource, int statusCode) {
		this.resourceType = resourceType;
		this.resource = resource;
		this.statusCode = statusCode;
	}

	public String getResourceType() {
		return resourceType;
	}

	public String getResource() {
		return resource;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
