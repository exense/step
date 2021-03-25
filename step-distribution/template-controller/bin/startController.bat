@rem ***************************************************************************
@rem Copyright (C) 2020, exense GmbH
@rem  
@rem This file is part of STEP
@rem  
@rem STEP is free software: you can redistribute it and/or modify
@rem it under the terms of the GNU Affero General Public License as published by
@rem the Free Software Foundation, either version 3 of the License, or
@rem (at your option) any later version.
@rem  
@rem STEP is distributed in the hope that it will be useful,
@rem but WITHOUT ANY WARRANTY; without even the implied warranty of
@rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@rem GNU Affero General Public License for more details.
@rem  
@rem You should have received a copy of the GNU Affero General Public License
@rem along with STEP.  If not, see <http://www.gnu.org/licenses/>.
@rem ***************************************************************************
rem @echo off

TITLE STEP Controller

rem if java.exe isn't on your path or is too old, then set your own as follows (watch for the backslash and double quote at the end):
rem SET JAVA_PATH=C:\Program Files\Java\jdk-11.0.10\bin\
SET JAVA_PATH=

SET JAVA_OPTS=%JAVA_OPTS% -Dlogback.configurationFile=./logback.xml -Dhttp.keepAlive=true -Dhttp.maxConnections=100

SET LIB_PATH=..\lib

"%JAVA_PATH%java.exe" %JAVA_OPTS% -cp "%LIB_PATH%\*;" step.controller.ControllerServer -config=..\conf\step.properties