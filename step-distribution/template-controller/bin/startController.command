#!/bin/sh

JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

JAVA_OPTS="-Dlogback.configurationFile=./logback.xml -Dhttp.keepAlive=true -Dhttp.maxConnections=100"

${JAVA_PATH}java ${JAVA_OPTS} -cp ../lib/*: step.controller.ControllerServer -config=${ABSPATH}/../conf/step.properties