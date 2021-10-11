# Stream Technologies Bridge
This is a simple utility to use as an agent to forward messages from a stream technology to another.
At the moment it is possible to forward messages from Kafka to AWS Kinesis and viceversa.

## Build and run

The following sbt command will build a fat jar to use as standalone to run the agent:

```bash
sbt clean compile assembly
```

This is how to run the agent:

```bash
java -jar stream-technologies-bridge-0.0.1.jar $PWD/configfiledir  kafkaToKinesis
```

The agent requires 2 mandatory parameters:
* source directory of the config file
* bridge type. Current values are: `kafkToKinesis`, `kinesisToKafka`

### Config file

The config file to run the application has to be called `bridge.properties`. Here an example of configuration:

```
kafka.bootstrapServers=http://localhost:9092
kafka.groupId=BridgeConsumer
kinesis.endpoint=localhost:4566
kinesis.region=us-east-1
common.topics=gerrit_stream,gerrit_index
common.instanceId=d1942253-c88e-496b-b2f6-64c0706364c8
common.skipLocalMessages=true
```

Possible parameters are:

_Streams configuration:_
* kafka.bootstrapServers: Kafka consumer bootstrap server
* kafka.groupId: Kafka group id prefix
* kinesis.endpoint: Kinesis optional local endpoint to use for local debugging
* kinesis.region: Kinesis AWS region

_Common configuration_
* common.topics: name of the topics to consumer messages from, comma separated
* common.skipLocalMessages: when using the agent to forward messages between different Gerrit instances,
  to avoid loops, messages locally produced might not have to be published back
* common.instanceId: Gerrit instance id, used to determine the origin of the messages
