#!/bin/sh

JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

ABSPATH=$(cd "$(dirname "$0")"; pwd)

JAVA_OPTS="-DcollectorConfig=${ABSPATH}/../conf/Collector.xml -Dlogback.configurationFile=logback-collector.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp ${ABSPATH}/../lib/*: io.djigger.collector.server.Server