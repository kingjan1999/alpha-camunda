package me.kingjan1999.fhdw.alphacamunda.domain.xml;

import me.kingjan1999.fhdw.alphacamunda.domain.Activity;
import me.kingjan1999.fhdw.alphacamunda.domain.Event;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.List;

/**
 * XmlAdapter for transforming XES Events
 * to me.kingjan1999.fhdw.alphacamunda.fhdw.domain.Event
 * instances
 */
public class EventAdapter extends XmlAdapter<EventXml, Event> {
    private static final String ACTIVITY_KEY = "Activity";
    private static final String RESOURCE_KEY = "Resource";
    private static final String TIMESTAMP_KEY = "time:timestamp";

    @Override
    public Event unmarshal(EventXml v) {
        var activityName = v.getStringValue(ACTIVITY_KEY);
        var activity = Activity.getActivity(activityName);

        return new Event(
                activity,
                v.getStringValue(RESOURCE_KEY),
                v.getTimestampValue(TIMESTAMP_KEY));
    }

    @Override
    public EventXml marshal(Event v) {
        var attributes = List.of(
                new StringAttribute(ACTIVITY_KEY, v.getActivity().getName()),
                new StringAttribute(RESOURCE_KEY, v.getResource())
        );
        var dateAttributes = List.of(
                new DateAttribute(TIMESTAMP_KEY, v.getTimestamp())
        );

        return new EventXml(attributes, dateAttributes);
    }
}
