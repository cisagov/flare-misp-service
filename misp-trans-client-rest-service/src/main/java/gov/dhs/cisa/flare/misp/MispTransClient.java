package gov.dhs.cisa.flare.misp;

public class MispTransClient {

	private final long id;
	private final String status;
	private final String detailedStatus;
	private final String processType;
	private final String collection;
	private final String beginTimestamp;
	private final String endTimestamp;

	public MispTransClient(long id, String status, String detailedStatus, String processType, String collection,
			String beginTimestamp, String endTimestamp) {
		this.id = id;
		this.status = status;
		this.detailedStatus = detailedStatus;
		this.processType = processType;
		this.collection = collection;
		this.beginTimestamp = beginTimestamp;
		this.endTimestamp = endTimestamp;
	}

	public long getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public String getDetailedStatus() {
		return detailedStatus;
	}

	public String getProcessType() {
		return processType;
	}

	public String getCollection() {
		return collection;
	}

	public String getBeginTimestamp() {
		return beginTimestamp;
	}

	public String getEndTimeStamp() {
		return endTimestamp;
	}
}
