package me.kingjan1999.fhdw.alphacamunda;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * Utility XMLReader ignoring namespaces
 */
public class XMLReaderWithoutNamespace extends StreamReaderDelegate {
    public XMLReaderWithoutNamespace(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public String getAttributeNamespace(int arg0) {
        return "";
    }

    @Override
    public String getNamespaceURI() {
        return "";
    }

}
