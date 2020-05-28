package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Log;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parseFullLog() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        Log log = null;
        try (InputStream is = classloader.getResourceAsStream("parser-fixtures/eventlog-full.xes")) {
            log = Parser.parse(is);
        } catch (IOException | JAXBException | XMLStreamException e) {
            fail(e);
        }

        assertNotNull(log);

        assertEquals(6, log.getTraces().size());
        var firstTrace = log.getTraces().get(0);
        assertEquals(9, firstTrace.getEvents().size());
        var firstEvent = firstTrace.getEvents().get(0);
        assertEquals("Pete", firstEvent.getResource());
        assertEquals("register request", firstEvent.getActivity().getName());
        var firstTimestamp = firstEvent.getTimestamp();
        assertEquals(Instant.parse("2010-12-30T13:32:00.000Z"), firstTimestamp);

        // sic! Assert that it's the identical object (i.e. not two instances are created for the same activity)
        assertSame(firstEvent.getActivity(), log.getTraces().get(1).getEvents().get(0).getActivity());
    }
}