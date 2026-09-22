package com.partner;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ticket {
    @JsonProperty("ticket_id")
    private String ticketId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("category")
    private String category;

    @JsonProperty("description")
    private String description;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("resolution_time_hours")
    private double resolutionTimeHours;

    @JsonProperty("satisfaction")
    private int satisfaction;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("is_resolved")
    private boolean isResolved;

    // Getters
    public String getTicketId() { return ticketId; }
    public String getCreatedAt() { return createdAt; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public double getResolutionTimeHours() { return resolutionTimeHours; }
    public int getSatisfaction() { return satisfaction; }
    public String getChannel() { return channel; }
    public boolean isResolved() { return isResolved; }

    public String getDate() {
        return createdAt.substring(0, 10);
    }
}
