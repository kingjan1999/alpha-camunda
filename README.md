# Alpha Algorithm for Camunda
[ ![Download](https://api.bintray.com/packages/kingjan1999/alpha-algorithm-camunda/alpha-camunda/images/download.svg) ](https://bintray.com/kingjan1999/alpha-algorithm-camunda/alpha-camunda/_latestVersion)

This library allows you to parse an eventlog (as XES file) and generate a Camunda compatible  BPMN model from it using the alph algorithm.

## Get

The library is available on [Bintray](https://bintray.com/kingjan1999/alpha-algorithm-camunda/alpha-camunda) and can be used with Maven and Gradle.
Alternatively, you can download the latest version on the releases page.

## Usage Example

### Parser

Use `Parser.parse(InputStream)` to create a event log representation from a given InputStream with XES-Data: 
```java
Log log = Parser.parse(file.getInputStream())
```

### RelationBuilder
Once you've got a log from the parser you can use the `RelationBuilder` to build the necessary relations for executing the alpha algorithm like this:
```java
RelationBuilder builder = new RelationBuilder();
builder.evaluate(log);
```

### BPMNCreator
With the filled `builder` you can use the `BPMNCreator` to finally create the layouted BPMN Model:
```java
BpmnModelInstance layoutedInstance = BPMNCreator.createAndLayout(algorithm);
```

### Complete Example

```java
Log log;
try {
    log = Parser.parse(file.getInputStream());
} catch (JAXBException | XMLStreamException | IOException e) {
    e.printStackTrace();
    return;
}

RelationBuilder algorithm = new RelationBuilder();
algorithm.evaluate(log);

BpmnModelInstance layoutedInstance = BPMNCreator.createAndLayout(algorithm);
```

## [Demo](https://alpha.jbeckmann.info/)