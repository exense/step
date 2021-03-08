#!/bin/sh

JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

ABSPATH=$(cd "$(dirname "$0")"; pwd)

cd $ABSPATH

JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=./logback.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp ${ABSPATH}/../lib/*: step.grid.agent.AgentRunner -config=${ABSPATH}/../conf/AgentConf.yaml