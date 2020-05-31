package me.kingjan1999.fhdw.alphacamunda.layouter;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

/**
 * Generic interface for layouters
 * Layouters take a BpmnModelInstance
 * and return a BpmnModelInstance when layout() is called
 *
 * @see FluentLayouter
 */
public interface Layouter {

    /**
     * Creates a diagramm layout for the given modelInstance
     * Note: A new modelInstance is created and the old remains untouched.
     * @param modelInstance ModelInstance to layout
     * @return Layouted modelInstance
     */
    BpmnModelInstance layout(BpmnModelInstance modelInstance);
}
