package me.kingjan1999.fhdw.alphacamunda.layouter;

import me.kingjan1999.fhdw.alphacamunda.BPMNUtil;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Layouter for BPMN processes
 * Uses layouting in camundas fluent builder API
 */
@SuppressWarnings("rawtypes") // Camunda API is "broken"
public class FluentLayouter implements Layouter {
    private final Process process;

    private static final Logger logger = LoggerFactory.getLogger(FluentLayouter.class);
    private Set<FlowNode> done;
    private List<FlowNode> canBeDone;

    public FluentLayouter(BpmnModelInstance modelInstance) {
        process = modelInstance.getModelElementsByType(Process.class).iterator().next();
    }

    @Override
    public BpmnModelInstance layout() {
        ProcessBuilder processBuilder = Bpmn.createExecutableProcess();
        var startEvent = process.getModelInstance().getModelElementsByType(StartEvent.class).iterator().next();
        var allElements = process.getModelInstance().getModelElementsByType(FlowNode.class);

        AbstractFlowNodeBuilder builder = processBuilder.startEvent(startEvent.getId());

        done = new HashSet<>();
        canBeDone = new ArrayList<>();

        builder = buildAtNode(builder, startEvent);

        // Place unplaced elements randomly on the plane
        while (done.size() < allElements.size()) {
            var newProcess = builder.done().getModelElementsByType(Process.class).iterator().next();
            var ultimateXBounds = builder.done().getModelElementsByType(BpmnShape.class).stream()
                    .mapToDouble(x -> x.getBounds().getX())
                    .max().orElse(0);

            var lastXBounds = ultimateXBounds + 200;
            Comparator<FlowNode> comp = Comparator.comparingInt(x -> x.getOutgoing().size());

            var stillToAdd = allElements.stream() // NOSONAR We already check for the presence (done.size < allElements.size)
                    .filter(x -> !done.contains(x)).max(comp).get();

            var classToCreate = getClassToCreate(stillToAdd);
            var element = BPMNUtil.createElement(newProcess, stillToAdd.getId(), classToCreate);
            var shape = builder.createBpmnShape(element);
            shape.getBounds().setX(lastXBounds);

            // probably redundant, but harmless (because we have a set)
            done.add(stillToAdd);

            // maybe we can place more elements now
            builder = buildAtNode(builder, stillToAdd);
        }

        assert done.size() == allElements.size();

        return builder.done();
    }

    private AbstractFlowNodeBuilder buildAtNode(AbstractFlowNodeBuilder builder, FlowNode currentNode) {
        while (currentNode != null) {
            builder = buildForNode(currentNode, builder);
            done.add(currentNode);

            canBeDone.addAll(currentNode.getOutgoing().stream()
                    .map(SequenceFlow::getTarget)
                    .filter(x -> !done.contains(x))
                    .collect(Collectors.toList()));
            canBeDone.remove(currentNode);


            currentNode = currentNode.getOutgoing().stream()
                    .map(SequenceFlow::getTarget)
                    .filter(x -> !done.contains(x))
                    .findFirst().orElse(null);
            if (currentNode == null) {
                currentNode = canBeDone.stream()
                        .filter(x -> !done.contains(x))
                        .findFirst().orElse(null);
            }
        }
        return builder;
    }

    private Class<? extends FlowNode> getClassToCreate(FlowNode x) {
        if (x instanceof ParallelGateway) {
            return ParallelGateway.class;
        }

        if (x instanceof ExclusiveGateway) {
            return ExclusiveGateway.class;
        }

        if (x instanceof UserTask || x instanceof ServiceTask) {
            return UserTask.class;
        }

        if (x instanceof EndEvent) {
            return EndEvent.class;
        }

        throw new IllegalArgumentException(x.getClass().toString());
    }

    private AbstractFlowNodeBuilder buildForNode(FlowNode node, AbstractFlowNodeBuilder builder) {
        for (var next : node.getOutgoing()) {
            builder = builder.moveToNode(node.getId());

            var nextElement = next.getTarget();
            if (nextElement == null) {
                logger.debug("Next Element is null: {}, {}", next.getId(), node.getId());
                continue;
            }

            var existsAlready = builder.done().getModelElementById(nextElement.getId()) != null;
            if (existsAlready) {
                builder = builder.connectTo(nextElement.getId());
            } else {
                builder = addAsNext(builder, nextElement);
            }
        }
        return builder.moveToNode(node.getId());
    }

    private AbstractFlowNodeBuilder addAsNext(AbstractFlowNodeBuilder builder, FlowNode nextElement) {
        if (nextElement instanceof ServiceTask || nextElement instanceof UserTask) {
            builder = builder.userTask(nextElement.getId());
        } else if (nextElement instanceof ExclusiveGateway) {
            builder = builder.exclusiveGateway(nextElement.getId());
        } else if (nextElement instanceof ParallelGateway) {
            builder = builder.parallelGateway(nextElement.getId());
        } else if (nextElement instanceof EndEvent) {
            builder = builder.endEvent(nextElement.getId());
        } else {
            throw new UnsupportedOperationException(nextElement.getElementType().getTypeName());
        }
        return builder;
    }
}
