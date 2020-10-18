# Alpha Algorithm for Camunda
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.kingjan1999.fhdw/alphacamunda/badge.svg)](https://search.maven.org/artifact/me.kingjan1999.fhdw/alphacamunda)
[![Build Status](https://travis-ci.org/kingjan1999/alpha-camunda.svg?branch=master)](https://travis-ci.org/kingjan1999/alpha-camunda)

This library allows you to parse an eventlog (as XES file) and generate a Camunda compatible  BPMN model from it using the alpha algorithm.

## Get

The library is available on [Maven Central](https://search.maven.org/artifact/me.kingjan1999.fhdw/alphacamunda) and JCenter and can be used with Maven and Gradle.
```xml
<dependency>
  <groupId>me.kingjan1999.fhdw</groupId>
  <artifactId>alphacamunda</artifactId>
  <version>1.0.4</version>
</dependency>
```

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
