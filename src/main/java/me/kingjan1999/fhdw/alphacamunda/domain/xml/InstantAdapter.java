package me.kingjan1999.fhdw.alphacamunda.domain.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantAdapter  extends XmlAdapter<String, Instant> {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Override
    public String marshal(Instant dateTime) {
        return dateFormat.format(dateTime);
    }

    @Override
    public Instant unmarshal(String dateTime) {
        return dateFormat.parse(dateTime, Instant::from);
    }

}
