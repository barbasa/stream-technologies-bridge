# Stream Technologies Bridge
This is a simple utility to use as an agent to forward messages from a stream technology to another.
At the moment it is possible to forward messages from Kafka to AWS Kinesis and viceversa.

## Build and run

The following sbt command will build a fat jar to use as standalone to run the agent:

```bash
sbt clean assembly
```

This is how to run the agent:

```bash
java -jar stream-technologies-bridge-*.jar kafkaToKinesis
```

The agent requires a mandatory parameter:
* bridge type. Current values are: `kafkToKinesis`, `kinesisToKafka`

### Environment variables

Several environment variables are available to configure the bridge:

_Streams configuration:_
* `BRIDGE_KAFKA_BOOTSTRAPSERVERS`: Kafka consumer bootstrap server
* `BRIDGE_KAFKA_GROUPID`: Kafka group id prefix
* `BRIDGE_KINESIS_ENDPOINT`: Kinesis optional local endpoint to use for local debugging
* `BRIDGE_KINESIS_REGION`: Kinesis AWS region
* `BRIDGE_KINESIS_APPNAME`: Kinesis AWS application name

_Common configuration_
* `BRIDGE_COMMON_TOPICS`: name of the topics to consume messages from, comma separated
* `BRIDGE_COMMON_ONLY_FORWARD_LOCAL_MESSAGES`: when using the agent to forward messages between different Gerrit instances,
  to avoid loops, only messages locally produced might have to be published back
* `BRIDGE_COMMON_INSTANCEID`: Gerrit instance id, used to determine the origin of the messages
