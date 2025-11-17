package com.myapp.loco;

import javafx.beans.property.SimpleStringProperty;

public class LogEvent {
    private final SimpleStringProperty eventId;
    private final SimpleStringProperty timeCreated;
    private final SimpleStringProperty providerName;
    private final SimpleStringProperty level;
    private final SimpleStringProperty description;
    private final String fullDetails;

    public LogEvent(String eventId, String timeCreated, String providerName, String level, String description, String fullDetails) {
        this.eventId = new SimpleStringProperty(eventId);
        this.timeCreated = new SimpleStringProperty(timeCreated);
        this.providerName = new SimpleStringProperty(providerName);
        this.level = new SimpleStringProperty(level);
        this.description = new SimpleStringProperty(description);
        this.fullDetails = fullDetails;
    }

    //Getters

    public String getEventId() {
        return eventId.get();
    }

    public SimpleStringProperty eventIdProperty() {
        return eventId;
    }

    public String getTimeCreated() {
        return timeCreated.get();
    }

    public SimpleStringProperty timeCreatedProperty() {
        return timeCreated;
    }

    public String getProviderName() {
        return providerName.get();
    }

    public SimpleStringProperty providerNameProperty() {
        return providerName;
    }

    public String getLevel() {
        return level.get();
    }

    public SimpleStringProperty levelProperty() {
        return level;
    }

    public String getDescription() {
        return description.get();
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public String getFullDetails() {
        return fullDetails;
    }
}

