#!/bin/sh

JAVA_PATH=""
JAVA_OPTS="-Dlogback.configurationFile=./logback.xml"
#JAVA_OPTS="{JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
${JAVA_PATH}java ${JAVA_OPTS} -cp "../lib/*" step.grid.agent.AgentRunner -config="../conf/AgentConf.yaml" "$@"
