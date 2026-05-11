#!/bin/sh
set -eu

BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-kafka:19092}"
TOPICS="${KAFKA_TOPICS:-task-events:3:1 device-commands:3:1 asset-events:3:1 uav-telemetry:3:1 agent-events:3:1 dead-letter-events:3:1}"

for topic_spec in $TOPICS; do
  topic_name="$(echo "$topic_spec" | cut -d: -f1)"
  partitions="$(echo "$topic_spec" | cut -d: -f2)"
  replication_factor="$(echo "$topic_spec" | cut -d: -f3)"

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP_SERVERS" \
    --create \
    --if-not-exists \
    --topic "$topic_name" \
    --partitions "$partitions" \
    --replication-factor "$replication_factor"
done

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVERS" --list
