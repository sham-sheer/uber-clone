package com.example.etows.historyRecyclerView;

public class HistoryObject {
    private String rideId;
    private String timestamp;

    public HistoryObject(String rideId, String timestamp) {
        this.rideId = rideId;
        this.timestamp = timestamp;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
