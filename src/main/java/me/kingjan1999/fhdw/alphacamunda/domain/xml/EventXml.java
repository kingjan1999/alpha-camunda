package me.kingjan1999.fhdw.alphacamunda.domain.xml;

import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Subset of the event represenation
 * used in XES.
 * <p>
 * Used for (un)marshalling
 */
public class EventXml {

    @XmlElement(name = "string")
    private final List<StringAttribute> stringAttributes;

    @XmlElement(name = "date")
    private final List<DateAttribute> dateAttributes;

    public EventXml(List<StringAttribute> stringAttributes, List<DateAttribute> dateAttributes) {
        this.stringAttributes = stringAttributes;
        this.dateAttributes = dateAttributes;
    }

    public EventXml() {
        this.stringAttributes = new ArrayList<>();
        this.dateAttributes = new ArrayList<>();
    }

    public String getStringValue(String key) {
        return stringAttributes.stream()
                .filter(x -> x.getKey().equals(key))
                .map(StringAttribute::getValue)
                .findFirst()
                .orElse(null);
    }

    public Instant getTimestampValue(String key) {
        return dateAttributes.stream()
                .filter(x -> x.getKey().equals(key))
                .map(DateAttribute::getValue)
                .findFirst()
                .orElse(null);
    }
}
