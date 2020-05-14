#!/bin/sh
#*******************************************************************************
# (C) Copyright 2016 Jerome Comte and Dorian Cransac
#  
# This file is part of STEP
#  
# STEP is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#  
# STEP is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#  
# You should have received a copy of the GNU Affero General Public License
# along with STEP.  If not, see <http://www.gnu.org/licenses/>.
#*******************************************************************************
JAVA_PATH=""
#JAVA_PATH="/usr/sbin/jre1.8.0_77/bin/"

JAVA_OPTS="-Dlogback.configurationFile=./logback.xml -Dhttp.keepAlive=true -Dhttp.maxConnections=100 -Dnashorn.args=\"--no-deprecation-warning\""

# the classpath should be absolute:
FILE_FULLPATH=$(readlink -f "$0")
FILE_PATH=$(dirname "${FILE_FULLPATH}")
LIB_PATH=${FILE_PATH}/../lib

${JAVA_PATH}java ${JAVA_OPTS} -cp "${LIB_PATH}/*:" step.controller.ControllerServer -config=../conf/step.properties
