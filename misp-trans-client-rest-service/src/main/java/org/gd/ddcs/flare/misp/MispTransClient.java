package org.gd.ddcs.flare.misp;

public class MispTransClient {

    private final long id;
    private final String status;
    private final String detailedStatus;
    private final String processType;
    private final String collection;

    public MispTransClient(long id, 
    		               String status,
    		               String detailedStatus,
    		               String processType, 
    		               String collection) {
        this.id = id;
        this.status = status;
        this.detailedStatus = detailedStatus;
        this.processType = processType;
        this.collection = collection;
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
}
