package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Trace;
import me.kingjan1999.fhdw.alphacamunda.domain.Log;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

public class Parser {

    private Parser() {}

    /**
     * Parses the given inputstream containing xml to an activity log
     *
     * @param xmlStream Stream containing valid XML data
     * @return The parsed log
     * @throws JAXBException      Thrown if errors regarding JAXB are encountered
     * @throws XMLStreamException Thrown if an unexpected processing error occurs
     */
    public static Log parse(InputStream xmlStream) throws JAXBException, XMLStreamException {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        XMLStreamReader xsr = factory.createXMLStreamReader(xmlStream);
        XMLReaderWithoutNamespace xr = new XMLReaderWithoutNamespace(xsr);

        JAXBContext jc = JAXBContext.newInstance(Log.class, Trace.class);

        Unmarshaller u = jc.createUnmarshaller();
        Object o = u.unmarshal(xr);
        return (Log) o;
    }
}
