JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

JAVA_OPTS="-DcollectorConfig=../conf/Collector.xml -Dlogback.configurationFile=logback-collector.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp "../lib/*" io.djigger.collector.server.Server