JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

JAVA_OPTS=-Dlogback.configurationFile=logback-client.xml

${JAVA_PATH}java ${JAVA_OPTS} -cp "../lib/*" io.djigger.ui.MainFrame