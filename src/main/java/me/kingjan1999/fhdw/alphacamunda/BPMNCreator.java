package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Activity;
import me.kingjan1999.fhdw.alphacamunda.layouter.FluentLayouter;
import me.kingjan1999.fhdw.alphacamunda.layouter.Layouter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.kingjan1999.fhdw.alphacamunda.BPMNUtil.createElement;

/**
 * Responsible for transforming relations into a
 * BPMN model
 */
public class BPMNCreator {

    private static final Logger logger = LoggerFactory.getLogger(BPMNCreator.class);
    private Process process;
    private Map<Activity, FlowNode> mappedActivities;

    private static final String ALPHA_NAME = "alpha";
    private static final String EPSILON_NAME = "epsilon";

    /**
     * Creates a new layouted bpmn model based on the results
     * of the passed {@link RelationBuilder}.
     * @param algorithm Filled RelationBuilder
     * @return Layouted generated BpmnModelInstance
     */
    public static BpmnModelInstance createAndLayout(RelationBuilder algorithm) {
        var creator = new BPMNCreator();
        var modelInstance = creator.create(
                algorithm.getActivityList(),
                algorithm.getAlternatives(),
                algorithm.getAbstractions(),
                algorithm.getRemainingCausalities(),
                algorithm.getNotSuccession()
        );

        BpmnModelInstance layoutedInstance = doLayout(modelInstance);

        creator.addConditions(layoutedInstance);

        return layoutedInstance;
    }

    /**
     * Creates a new bpmn model based on the passed results
     * of the alpha algorithm so far
     * @param act Act Relation
     * @param alternatives Alternatives Relation
     * @param abstractions Abstractions Relation
     * @param causalities Causalities Relation
     * @param noSuccession NoSuccession Relation
     * @return Generated BpmnModelInstance, without layout
     */
    public BpmnModelInstance create(List<Activity> act,
                                    List<Triple<Activity, Activity, Activity>> alternatives,
                                    List<Triple<Activity, Activity, Activity>> abstractions,
                                    List<Pair<Activity, Activity>> causalities,
                                    List<Pair<Activity, Activity>> noSuccession) {
        ProcessBuilder builder = Bpmn.createExecutableProcess();
        var modelInstance = builder
                .startEvent()
                .name(ALPHA_NAME)
                .id(ALPHA_NAME)
                .endEvent()
                .name(EPSILON_NAME)
                .id(EPSILON_NAME)
                .done();

        process = modelInstance.getModelElementsByType(Process.class).iterator().next();

        // We hat to create a flow between start and end event above
        // This was wrong, so we remove it here
        process.removeChildElement(process.getModelInstance().getModelElementsByType(SequenceFlow.class).iterator().next());

        mappedActivities = new HashMap<>();
        var mappedStartEvent = process.getChildElementsByType(StartEvent.class).iterator().next();
        mappedActivities.put(Activity.getFakeStart(), mappedStartEvent);

        var mappedEndEvent = process.getChildElementsByType(EndEvent.class).iterator().next();
        mappedActivities.put(Activity.getFakeEnd(), mappedEndEvent);

        // Step 1
        act.stream().filter(x -> !mappedActivities.containsKey(x)).forEach(x -> {
            var element = createElement(process, x.getName(), ServiceTask.class);
            mappedActivities.put(x, element);
        });

        List<ExclusiveGateway> xorSplits = new ArrayList<>(alternatives.size());

        // Step 2
        createCausalities(causalities);

        // Step 3
        createAlternatives(alternatives, xorSplits);

        // Step 4
        createAbstractions(abstractions);

        // Step 5
        bundleActivtyFlows();

        // Step 6
        bundleGatewayFlows(noSuccession, xorSplits);

        // Step 7
        mergeGateways();
        deleteRedundantGateways();

        Bpmn.validateModel(modelInstance);
        return modelInstance;
    }


    /**
     * Removes redundant gateways (one input and one output)
     * from the model
     */
    private void deleteRedundantGateways() {
        AtomicBoolean someChange = new AtomicBoolean(true);
        while (someChange.get()) {
            someChange.set(false);
            var allGateways = process.getModelInstance().getModelElementsByType(Gateway.class);
            for (var gateway : allGateways) {
                if (gateway.getIncoming().size() == 1 && gateway.getOutgoing().size() == 1) {
                    safeDeleteNode(gateway);
                    someChange.set(true);
                }
            }
        }
    }

    /**
     * Merges redundant gateways (same inputs or same outputs)
     */
    private void mergeGateways() {
        // we only execute this operation pairwise
        // but maybe there are > 2 redundant gateways
        // That's why we do it repeatedly as long as nothing changed

        AtomicBoolean someChange = new AtomicBoolean(true);
        while (someChange.get()) {
            someChange.set(false);
            List<Gateway> deletedGateways = new ArrayList<>();
            var allGateways = process.getModelInstance().getModelElementsByType(Gateway.class);
            for (var gateway : allGateways) {
                var isSplit = gateway.getOutgoing().size() > 1;
                var filteredGateways = allGateways.stream().filter(x -> {
                    // Gateways is the same, doesn't exist anymore or is of an other type
                    return !x.equals(gateway) && !deletedGateways.contains(x) && x.getClass().equals(gateway.getClass());
                });

                if (isSplit) {
                    // We merge splits with other splits having the same inputs
                    var otherWithSameInput = filteredGateways.filter(x -> {
                        // compare incoming nddes
                        List<FlowNode> allIncomingLeft = x.getIncoming().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                        List<FlowNode> allIncomingRight = gateway.getIncoming().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                        return allIncomingLeft.equals(allIncomingRight);
                    }).findFirst();

                    otherWithSameInput.ifPresent(otherGateway -> {
                        // delete both gateways and create a new one instead
                        deletedGateways.add(otherGateway);
                        deletedGateways.add(gateway);
                        mergeIncoming(gateway, otherGateway);
                        someChange.set(true);
                    });
                } else {
                    // Joins are merged with other joins
                    // having the same outputs
                    var otherWithSameOutput = filteredGateways.filter(x -> {
                        List<FlowNode> allOutgoingLeft = x.getOutgoing().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                        List<FlowNode> allOutgoingRight = gateway.getOutgoing().stream().map(SequenceFlow::getSource).collect(Collectors.toList());
                        return allOutgoingLeft.equals(allOutgoingRight);
                    }).findFirst();

                    otherWithSameOutput.ifPresent(otherGateway -> {
                        deletedGateways.add(otherGateway);
                        deletedGateways.add(gateway);
                        mergeOutgoing(gateway, otherGateway);
                        someChange.set(true);
                    });
                }
            }

            // we could use iterators to delete in-place (and avoid CMEs)
            // but this way avoids the iterator
            for (var gatewayToDelete : deletedGateways) {
                process.removeChildElement(gatewayToDelete);
            }
        }
    }

    /**
     * Bundles splits with more than one input
     * and joins with more than one output
     *
     * @param noSuccession noSuccession relation from phase 1
     * @param xorSplits    List of all XOR-splits (necessary for distinguishing splits and joins)
     */
    private void bundleGatewayFlows(List<Pair<Activity, Activity>> noSuccession, List<ExclusiveGateway> xorSplits) {
        var allXorGateways = process.getModelInstance().getModelElementsByType(ExclusiveGateway.class);

        for (var gateway : allXorGateways) {
            var isSplit = xorSplits.contains(gateway);
            if (isSplit) {
                var incoming = new ArrayList<>(gateway.getIncoming());
                if (incoming.size() < 2) {
                    continue;
                }

                // XXX: This is still a (theoretical)
                // we only look at the first two incoming gateways, but merge all of them
                var first = incoming.get(0);
                var second = incoming.get(1);
                var isNoSuccession = noSuccession.contains(Pair.of(first, second)) || noSuccession.contains(Pair.of(second, first));
                if (isNoSuccession) {
                    bundleIncoming(gateway, ExclusiveGateway.class);
                } else {
                    bundleIncoming(gateway, ParallelGateway.class);
                }
            } else {
                var outgoing = new ArrayList<>(gateway.getOutgoing());

                if (outgoing.size() < 2) {
                    continue;
                }

                var first = outgoing.get(0);
                var second = outgoing.get(1);
                var isNoSuccession = noSuccession.contains(Pair.of(first, second)) || noSuccession.contains(Pair.of(second, first));
                if (isNoSuccession) {
                    bundleOutgoing(gateway, ExclusiveGateway.class);
                } else {
                    bundleOutgoing(gateway, ParallelGateway.class);
                }
            }
        }
    }

    /**
     * Bundles inputs and outputs of activites
     * having more than one input / output (step 5)
     */
    private void bundleActivtyFlows() {
        var allModelActivites = process.getModelInstance().getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Activity.class);
        for (var activity : allModelActivites) {
            bundleIncoming(activity, ParallelGateway.class);
            bundleOutgoing(activity, ParallelGateway.class);
        }
    }

    /**
     * Creates abstractions (i.e. XOR-Joins)
     * (Step 4)
     *
     * @param abstractions abstraction list created in phase 2 (X_{3-})
     */
    private void createAbstractions(List<Triple<Activity, Activity, Activity>> abstractions) {
        for (int i = 0; i < abstractions.size(); i++) {
            var alternative = abstractions.get(i);
            var mappedLeft = mappedActivities.get(alternative.getLeft());
            var mappedMiddle = mappedActivities.get(alternative.getMiddle());
            var mappedRight = mappedActivities.get(alternative.getRight());

            var xorGateway = createElement(process, "xor-abstr-" + i, ExclusiveGateway.class);
            createSequenceFlow(mappedLeft, xorGateway);
            createSequenceFlow(mappedMiddle, xorGateway);
            createSequenceFlow(xorGateway, mappedRight);
        }
    }

    /**
     * Creates alternatives (i.e. XOR-Splits)
     * (Step 3)
     *
     * @param alternatives alternatives list created in phase 2 (X_{3+})
     * @param xorSplits    list of all xor-splits (necessary for distinguishing splits and joins)
     */
    private void createAlternatives(List<Triple<Activity, Activity, Activity>> alternatives, List<ExclusiveGateway> xorSplits) {
        for (int i = 0; i < alternatives.size(); i++) {
            var alternative = alternatives.get(i);
            var mappedLeft = mappedActivities.get(alternative.getLeft());
            var mappedMiddle = mappedActivities.get(alternative.getMiddle());
            var mappedRight = mappedActivities.get(alternative.getRight());

            var xorGateway = createElement(process, "xor-alt-" + i, ExclusiveGateway.class);
            createSequenceFlow(mappedLeft, xorGateway);
            createSequenceFlow(xorGateway, mappedMiddle);
            createSequenceFlow(xorGateway, mappedRight);
            xorSplits.add(xorGateway);
        }
    }

    /**
     * Creates causalities between to activities (step 2)
     *
     * @param causalities list of all remaining causalitiescausalities (from phase 2, X_2)
     */
    private void createCausalities(List<Pair<Activity, Activity>> causalities) {
        for (var causality : causalities) {
            var left = causality.getLeft();
            var mappedLeft = mappedActivities.get(left);

            var right = causality.getRight();
            var mappedRight = mappedActivities.get(right);
            createSequenceFlow(mappedLeft, mappedRight);
        }
    }

    /**
     * Safely removes the node from the process
     * Safe: incoming nodes are connected with outgoing nodes
     *
     * @param node Node to delete
     */
    private void safeDeleteNode(FlowNode node) {
        var incomingFlow = node.getIncoming().iterator().next();
        var outgoingFlow = node.getOutgoing().iterator().next();
        incomingFlow.setTarget(outgoingFlow.getTarget());
        outgoingFlow.getTarget().getIncoming().remove(outgoingFlow);
        incomingFlow.getTarget().getIncoming().add(incomingFlow);
        process.removeChildElement(outgoingFlow);
        process.removeChildElement(node);
    }

    /**
     * Creates a new gateway based on the names of the old gateways
     * Attention: Old gateways are <b>not</b> getting deleted
     *
     * @param gateway1 Gateway 1
     * @param gateway2 Gateway 2
     * @param <T>      Type of the old and new gateways
     * @return  The new gateway
     */
    private <T extends Gateway> Gateway createMergeGateway(T gateway1, T gateway2) {
        String id = gateway1.getId() + "_" + gateway2.getId() + "-merged";
        Class<? extends Gateway> classToCreate = ExclusiveGateway.class;
        if (gateway1 instanceof ParallelGateway) {
            classToCreate = ParallelGateway.class;
        }
        return createElement(process, id, classToCreate);
    }

    /**
     * Merges both gateways
     * <em>Incoming</em> connections are bundled
     *
     * @param gateway1 Gateway 1
     * @param gateway2 Gateway 2
     * @param <T>      Type of the gateways
     */
    private <T extends Gateway> void mergeIncoming(T gateway1, T gateway2) {
        var newGateway = createMergeGateway(gateway1, gateway2);
        gateway1.getIncoming().forEach(x -> {
            x.setTarget(newGateway);
            newGateway.getIncoming().add(x);
        });

        gateway2.getIncoming().forEach(x -> {
            x.getSource().getOutgoing().remove(x);
            process.removeChildElement(x);
        });

        List<FlowNode> alreadyAddedTargets = new ArrayList<>();

        Stream.concat(gateway1.getOutgoing().stream(), gateway2.getOutgoing().stream()).forEach(x -> {
            if (alreadyAddedTargets.contains(x.getTarget())) {
                process.removeChildElement(x);
                x.getTarget().getIncoming().remove(x);
                return;
            }


            alreadyAddedTargets.add(x.getTarget());
            x.setSource(newGateway);
            newGateway.getOutgoing().add(x);
        });
    }

    /**
     * Merges both gateways
     * <em>Outgoing</em> connections are bundled
     *
     * @param gateway1 Gateway 1
     * @param gateway2 Gateway 2
     * @param <T>      Type of the gateways
     */
    private <T extends Gateway> void mergeOutgoing(T gateway1, T gateway2) {
        var newGateway = createMergeGateway(gateway1, gateway2);

        gateway1.getOutgoing().forEach(x -> {
            x.setSource(newGateway);
            newGateway.getOutgoing().add(x);
        });

        gateway2.getOutgoing().forEach(x -> {
            x.getTarget().getIncoming().remove(x);
            process.removeChildElement(x);
        });

        Stream.concat(gateway1.getIncoming().stream(), gateway2.getIncoming().stream()).forEach(x -> {
            x.setTarget(newGateway);
            newGateway.getIncoming().add(x);
        });
    }

    /**
     * Bundles the incoming connections of this node into a new node
     *
     * @param affectedNode Affected node
     * @param elementClass Class of the new node (usally ParallelGateway)
     * @param <T>          Type of the new node
     */
    private <T extends FlowNode> void bundleIncoming(FlowNode affectedNode, Class<T> elementClass) {
        var incoming = new ArrayList<>(affectedNode.getIncoming());
        if (incoming.size() < 2) {
            return;
        }

        var newParallelGateway = createElement(process, affectedNode.getId() + "-prfx-bndl-in", elementClass);
        createSequenceFlow(newParallelGateway, affectedNode);
        incoming.forEach(x -> {
            x.setTarget(newParallelGateway);
            newParallelGateway.getIncoming().add(x);
            affectedNode.getIncoming().remove(x);
        });
    }

    /**
     * Bundles the outgoing connections of this node into a new node
     *
     * @param affectedNode Affected node
     * @param elementClass Class of the new node (usally ParallelGateway)
     * @param <T>          Type of the new node
     */
    private <T extends FlowNode> void bundleOutgoing(FlowNode affectedNode, Class<T> elementClass) {
        var outgoing = new ArrayList<>(affectedNode.getOutgoing());
        if (outgoing.size() < 2) {
            return;
        }

        var newParallelGateway = createElement(process, affectedNode.getId() + "-prfx-bndl-out", elementClass);
        createSequenceFlow(affectedNode, newParallelGateway);
        outgoing.forEach(x -> {
            x.setSource(newParallelGateway);
            newParallelGateway.getOutgoing().add(x);
            affectedNode.getOutgoing().remove(x);
        });
    }


    private SequenceFlow createSequenceFlow(FlowNode from, FlowNode to) {
        return BPMNUtil.createSequenceFlow(process, from, to);
    }

    /**
     * Adds a some conditions to all XOR-Splits having more than one output
     *
     * @param modelInstance
     */
    private void addConditions(BpmnModelInstance modelInstance) {
        modelInstance.getModelElementsByType(SequenceFlow.class).forEach(x -> {

            if (x.getSource() instanceof ExclusiveGateway) {
                var silbings = new ArrayList<>(x.getSource().getOutgoing());
                if (silbings.size() > 1) {
                    for (int i = 1; i < silbings.size(); i++) {
                        SequenceFlow y = silbings.get(i);
                        var condExpr = modelInstance.newInstance(ConditionExpression.class);
                        condExpr.setTextContent("${action == " + i + "}");
                        y.setConditionExpression(condExpr);
                    }
                }
            }
        });
    }

    private static BpmnModelInstance doLayout(BpmnModelInstance modelInstance) {
        return new FluentLayouter().layout(modelInstance);
    }
}



  
  