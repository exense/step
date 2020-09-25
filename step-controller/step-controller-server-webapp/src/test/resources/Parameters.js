/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
[
["selenium.webdriver.firefox.bin","../ext/bin/firefox/FirefoxPortable_50.0.1_English/App/Firefox/firefox.exe","Java.type('java.lang.System').getProperty('os.name').startsWith('Windows')"],
["selenium.webdriver.gecko.driver","../ext/bin/geckodriver/geckodriver.exe","Java.type('java.lang.System').getProperty('os.name').startsWith('Windows')"],
["selenium.webdriver.phantomjs.driver","../ext/bin/phantomjs/phantomjs.exe","Java.type('java.lang.System').getProperty('os.name').startsWith('Windows')"],
["selenium.webdriver.firefox.bin","../ext/bin/firefox/FirefoxPortable_50.0.1_English/App/Firefox/firefox",""],
["selenium.webdriver.gecko.driver","../ext/bin/geckodriver/geckodriver",""],
["selenium.webdriver.phantomjs.driver","../ext/bin/phantomjs/phantomjs",""],
["scripthandler.script.dir","../data/scripts",""],
["tec.execution.reportnodes.persistbefore","true",""],
["tec.execution.reportnodes.persistafter","true",""],
["tec.execution.reportnodes.persistonlynonpassed","false",""],
["keywords.calltimeout.default",180000,""],
["version","1.3","env=='TEST'"],
["version","1.2","env=='PROD'"]
]
