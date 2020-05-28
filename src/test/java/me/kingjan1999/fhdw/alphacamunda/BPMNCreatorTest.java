package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Log;
import org.camunda.bpm.model.bpmn.instance.*;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BPMNCreatorTest {

    @Test
    void createAndLayoutSimple() {
        var builder = buildForFile("parser-fixtures/eventlog-partial.xes");

        assertEquals(3, builder.getActivityList().size()); // 1 in file + Alpha + Epsilon

        var createdModelInstance = BPMNCreator.createAndLayout(builder);
        assertEquals(1, createdModelInstance.getModelElementsByType(Activity.class).size());
        assertEquals(1, createdModelInstance.getModelElementsByType(StartEvent.class).size());
        assertEquals(1, createdModelInstance.getModelElementsByType(EndEvent.class).size());
        assertEquals(2, createdModelInstance.getModelElementsByType(SequenceFlow.class).size());
    }

    @Test
    void createAndLayoutFull() {
        var builder = buildForFile("parser-fixtures/eventlog-full.xes");

        assertEquals(10, builder.getActivityList().size()); // 8 in file + Alpha + Epsilon

        var createdModelInstance = BPMNCreator.createAndLayout(builder);
        assertEquals(8, createdModelInstance.getModelElementsByType(Activity.class).size());
        assertEquals(1, createdModelInstance.getModelElementsByType(StartEvent.class).size());
        assertEquals(1, createdModelInstance.getModelElementsByType(EndEvent.class).size());

        var startEvent = createdModelInstance.getModelElementsByType(StartEvent.class).iterator().next();
        var activityAfterStart = startEvent.getOutgoing().iterator().next().getTarget();
        assertEquals("registerrequest", activityAfterStart.getName());
    }

    @Test
    void testSkript1() {
        // Figure 45
        var log = Util.createLogFromStrings("abghjkil", "acdefgjhikl");
        var builder = new RelationBuilder();
        builder.evaluate(log);
        var createdModelInstance = BPMNCreator.createAndLayout(builder);

        assertEquals(12, createdModelInstance.getModelElementsByType(Activity.class).size());
        assertEquals(2, createdModelInstance.getModelElementsByType(ParallelGateway.class).size());
        assertEquals(2, createdModelInstance.getModelElementsByType(ExclusiveGateway.class).size());
        assertEquals(19, createdModelInstance.getModelElementsByType(SequenceFlow.class).size());
    }

    private RelationBuilder buildForFile(String fileName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        Log log = null;
        try (InputStream is = classloader.getResourceAsStream(fileName)) {
            log = Parser.parse(is);
        } catch (IOException | JAXBException | XMLStreamException e) {
            fail(e);
        }
        var builder = new RelationBuilder();
        builder.evaluate(log);
        return builder;
    }
}