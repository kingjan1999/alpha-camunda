package me.kingjan1999.fhdw.alphacamunda.domain.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

/**
 * Simple key-value mapping
 * with string as key and instant as date
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DateAttribute {

    @XmlAttribute(name = "value")
    @XmlJavaTypeAdapter(InstantAdapter.class)
    private final Instant value;
    @XmlAttribute(name = "key")
    private final String key;

    public DateAttribute(String key, Instant value) {
        this.key = key;
        this.value = value;
    }

    public DateAttribute() {
        key = "";
        value = null;
    }

    public String getKey() {
        return key;
    }

    public Instant getValue() {
        return value;
    }
}
