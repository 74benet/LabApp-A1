package org.example.nutzungszeitaufgabe.event;

import java.util.List;

public class EventResponse {
    private List<Event> events;

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
