package me.kingjan1999.fhdw.alphacamunda.layouter;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

/**
 * Generic interface for layouters
 * Layouters take a BpmnModelInstance (via the constructor)
 * and return a BpmnModelInstance when layout() is called
 *
 * @see FluentLayouter
 */
public interface Layouter {

    BpmnModelInstance layout();
}
