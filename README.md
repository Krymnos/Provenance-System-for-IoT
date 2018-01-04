# Provenance-System-for-IoT

## Required Reading:
In order to understand the API, one must first be familiar with the [provenance project](https://github.com/Krymnos/IDP/issues/21) and [DataModel](https://github.com/Krymnos/IDP/issues/11) more specifically.

![Provenance Data Model](https://image.ibb.co/mk7CXG/Data_Model_for_IDP_V2_1.png)

### ProvenanceContext:
Each node in the pipeline has one unique `ProvenanceContext` and it is responsible for actual storage of the provenance data.
To create ProvenanceContext:

```java
ProvenanceContext pc = ProvenanceContext.getOrCreate();
```

### Datapoint:
Datapoint is a type to represent provenance data point. There is always a unique provenance Datapoint for each data value and each provenance Datapoint has a unique auto-generated identifier, list of input data points along with their contribution type to this data point and also some context information.

One can create the Datapoint as follows:

```java
Datapoint dp = new Datapoint();
```

### InputDatapoint:
Each data item in an IOT system is either captured from sensors or it is created by applying transformations to other data values. So, this information should also be part of the provenance Datapoint and that is carried inside InputDatapoint. Each InputDatapoint has an id of some Datapoint as well the contribution/transformation type that when applied to that data value possibly along with some other data values caused this new data value.
One can get a list of InputDatapoint just by passing list of Datapoint ids to the getInputDatapoints method of the ProvenanceContext. If you do not specify the contribution type it will mark it simple (No aggregation).

```java
String[] inputDPIDs = ...
InputDatapoint[] inputDPs = pc.getInputDatapoints(inputDPIDs);
```

One can specify the contribution/transformation as a second argument:

```java
String[] inputDPIDs = ...
InputDatapoint[] inputDPs = pc.getInputDatapoints(inputDPIDs, "avg");
```

To set input data points:

```java
dp.setInputDatapoints(inputDPs);
```

### Context:
Each data item in an IOT system whether it is captured from sensors directly or created by applying some transformation to other data values is done in some context and that context information is captured inside Context.

To create Context object:

```java
Context context = new Context().builder().setAppName("myApp").setClassName("MainClass.java")
                    .setLineNo(11)
                    .setLocation(new Location("Berlin"))
                    .setSendTimestamp(new Date(1513045527000))
                    .setReceiveTimestamp(new Date(1513043127000))
                    .setTimestamp(new Date(1513043227000))
                    .build();
```
                    
To set context:

```java
dp.setContext(context);
```

To save the provenance data point, one should call the save method of the ProvenanceContext:

```java
pc.save(dp);
```
## Building

Execute `mvn clean install` to build, and create jars.
