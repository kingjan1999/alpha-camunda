package me.kingjan1999.fhdw.alphacamunda.domain.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Simple key-value mapping
 * where both key and value are strings
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class StringAttribute {

    @XmlAttribute(name = "value")
    public final String value;
    @XmlAttribute(name = "key")
    private final String key;

    public StringAttribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public StringAttribute() {
        key = "";
        value = "";
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
