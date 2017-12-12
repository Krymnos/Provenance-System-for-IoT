# Provenance-System-for-IoT

#Required Reading
In order to understand the API, one must first be familiar with the [provenance project](https://github.com/Krymnos/IDP/issues/21) and [DataModel](https://github.com/Krymnos/IDP/issues/11) more specifically.

![Provenance Data Model](https://image.ibb.co/cQcrxw/Data_Model_for_IDP_2.png)

## ProvenanceContext:
Each node in the pipeline has one unique `ProvenanceContext` and it is responsible for actual storage of the provenance data.
To create ProvenanceContext:

`ProvenanceContext pc = ProvenanceContext.getOrCreate();`

## Datapoint:
Datapoint is a type to represent provenance data point. There is always a unique provenance Datapoint for each data value and each provenance Datapoint has a unique auto-generated identifier, some input data points along with their contribution type to this data point and also some context information.

One can create the Datapoint as follows:

`Datapoint dp = new Datapoint();`

## InputDatapoint:
Each data item in an IOT system is either capture from sensors or it is created by applying transformations to other data values. So, this information should also be part of the provenance Datapoint and that is carried inside InputDatapoint. Each InputDatapoint has an id of some Datapoint as well the contribution/transformation that when applied to that data value possibly along with some other data values caused new data value.
One can get a list of InputDatapoint just by passing list of Datapoint ids to the getInputDatapoints method of the ProvenanceContext. If you do not specify the contribution type it will mark it simple (No aggregation).

`String[] inputDPIDs = ...
InputDatapoint[] inputDPs = pc.getInputDatapoints(inputDPIDs);`

One can specify the contribution/transformation as second argument:
`String[] inputDPIDs = ...
InputDatapoint[] inputDPs = pc.getInputDatapoints(inputDPIDs, "avg");`

To set input data points:
`dp.setInputDatapoints(inputDPs);`

## Context:
Each data item in an IOT system whether it is captured from sensors directly or created by applying some transformation to other data values is done in some context and that context information is captured inside Context.

To create Context object:
`Context context = new Context().builder().setAppName("myApp").setClassName("MainClass.java")
                    .setLineNo(11)
                    .setLocation(new Location("Berlin"))
                    .setSendTimestamp(new Date(1513045527000))
                    .setReceiveTimestamp(new Date(1513043127000))
                    .setTimestamp(new Date(1513043227000))
                    .build();`
                    
To set context:
`dp.setContext(context);`

To save the provenance data point, one should call the save method of the ProvenanceContext:
`pc.save(dp);`
