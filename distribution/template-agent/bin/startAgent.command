#!/bin/sh

JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

ABSPATH=$(cd "$(dirname "$0")"; pwd)

JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1100 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dlogback.configurationFile=logback-client.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp ${ABSPATH}/../lib/*: io.djigger.ui.MainFrame
