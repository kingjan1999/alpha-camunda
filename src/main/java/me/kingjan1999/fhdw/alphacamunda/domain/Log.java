package me.kingjan1999.fhdw.alphacamunda.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of multiple traces.
 *
 * @see Trace
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Log {

    @XmlElement(name = "trace", required = true)
    private final List<Trace> traces;

    public Log() {
        this.traces = new ArrayList<>();
    }

    public Log(List<Trace> traces) {
        this.traces = traces;
    }

    public List<Trace> getTraces() {
        return traces;
    }
}
