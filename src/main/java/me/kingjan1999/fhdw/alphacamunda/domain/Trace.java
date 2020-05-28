package me.kingjan1999.fhdw.alphacamunda.domain;

import me.kingjan1999.fhdw.alphacamunda.domain.xml.EventAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * An event trace as in XES,
 * consisting of a consecutive list of events
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Trace {

    @XmlElement(name = "event")
    @XmlJavaTypeAdapter(value = EventAdapter.class)
    private final List<Event> events;

    public Trace() {
        this.events = new ArrayList<>();
    }

    public Trace(List<Event> events) {
        this.events = events;
    }

    public List<Event> getEvents() {
        return events;
    }
}
