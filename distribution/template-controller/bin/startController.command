#!/bin/sh

JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

ABSPATH=$(cd "$(dirname "$0")"; pwd)

JAVA_OPTS="-Dlogback.configurationFile=logback.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp ${ABSPATH}/../lib/*: step.controller.ControllerServer -config=${ABSPATH}/../conf/step.properties