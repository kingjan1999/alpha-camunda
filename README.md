# Alpha Algorithm for Camunda

This library allows you to parse an eventlog (as XES file) and generate a Camunda compatible  BPMN model from it using the alph algorithm.

## Usage Example

## Parser

Use `Parser.parse(InputStream)` to create a event log representation from a given InputStream with XES-Data: 
```java
Log log = Parser.parse(file.getInputStream())
```

## RelationBuilder
