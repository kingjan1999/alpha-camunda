package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Activity;
import me.kingjan1999.fhdw.alphacamunda.domain.Log;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class RelationBuilderTest {


    private RelationBuilder testAlphaObject;

    @Test
    void testManager() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Log log = null;
        try (InputStream is = classloader.getResourceAsStream("parser-fixtures/eventlog-full.xes")) {
            log = Parser.parse(is);
        } catch (IOException | JAXBException | XMLStreamException e) {
            fail(e);
        }
        this.testAlphaObject = new RelationBuilder();
        this.testAlphaObject.evaluate(log);
        internResultsTest();
    }

    @Test
    void testSkript1() {
        // Example 1
        var log = Util.createLogFromStrings("abghjkil", "acdefgjhikl");
        var alphaObj = new RelationBuilder();
        alphaObj.evaluate(log);

        assertEquals(1, alphaObj.getAlternatives().size());
        assertEquals(1, alphaObj.getAbstractions().size());
        assertEquals(11, alphaObj.getRemainingCausalities().size());

        assertTrue(alphaObj.getAlternatives().contains(Triple.of(Activity.getActivity("a"), Activity.getActivity("b"), Activity.getActivity("c"))));
        assertTrue(
                alphaObj.getAbstractions().contains(Triple.of(Activity.getActivity("b"), Activity.getActivity("f"), Activity.getActivity("g"))) ||
                alphaObj.getAbstractions().contains(Triple.of(Activity.getActivity("f"), Activity.getActivity("b"), Activity.getActivity("g")))
        );
    }

    @Test
    void testSkript2() {
        var log = Util.createLogFromStrings("ab", "bc");
        var alphaObj = new RelationBuilder();
        alphaObj.evaluate(log);

        assertEquals(0, alphaObj.getAbstractions().size());
        assertEquals(0, alphaObj.getAlternatives().size());
        assertEquals(6, alphaObj.getRemainingCausalities().size());
    }


    private void internResultsTest() {
        System.out.println("Causalities:");
        writePairLog(this.testAlphaObject.getCausality());
        assertEquals(15, testAlphaObject.getCausality().size());
        System.out.println();

        System.out.println("Parallel:");
        writePairLog(this.testAlphaObject.getParallel());
        assertEquals(4, testAlphaObject.getParallel().size());
        System.out.println();

        System.out.println("Alternatives:");
        writeTripleLog(this.testAlphaObject.getAlternatives());
        assertEquals(5, testAlphaObject.getAlternatives().size());
        System.out.println();

        System.out.println("Abstractions:");
        writeTripleLog(this.testAlphaObject.getAbstractions());
        assertEquals(5, testAlphaObject.getAbstractions().size());
        System.out.println();

        System.out.println("remainingCausalities:");
        writePairLog(this.testAlphaObject.getRemainingCausalities());
        assertEquals(2, testAlphaObject.getRemainingCausalities().size());
    }

    private void writePairLog(List<Pair<Activity, Activity>> list) {
        for (var current : list) {
            System.out.println(current.getLeft().getName() + "," + current.getRight().getName());
        }
    }

    private void writeTripleLog(List<Triple<Activity, Activity, Activity>> list) {
        for (var current : list) {
            System.out.println(current.getLeft().getName() + "," + current.getMiddle().getName() + ", " +
                    current.getRight().getName());
        }
    }

}