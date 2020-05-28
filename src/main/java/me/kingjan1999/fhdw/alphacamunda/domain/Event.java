package me.kingjan1999.fhdw.alphacamunda.domain;

import java.time.Instant;

/**
 * A single event, as occured in an event trace
 */
public class Event {

    private final Activity activity;

    private final String resource;

    private final Instant timestamp;

    public Event(Activity activity, String resource, Instant timestamp) {
        this.activity = activity;
        this.resource = resource;
        this.timestamp = timestamp;
    }

    public Activity getActivity() {
        return activity;
    }

    public String getResource() {
        return resource;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
