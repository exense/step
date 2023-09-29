#!/bin/sh
#*******************************************************************************
# Copyright (C) 2020, exense GmbH
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
#JAVA_PATH="/usr/sbin/jdk-11.0.10/bin/"

JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=./logback.xml"

${JAVA_PATH}java ${JAVA_OPTS} -cp "../lib/*" step.grid.agent.AgentRunner -config="../conf/AgentConf.yaml" "$@"
