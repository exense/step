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
 ******************************************************************************/
package step.datapool.gsheet;

import java.util.Map;

import org.junit.Assert;

import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class GoogleSheetv4DataPoolTest{


	//TODO: turned off because the saKey is too sensitive. Need a dummy account for public testing
	//@Test
	public void testGoogleSheetDataPool() {


		GoogleSheetv4DataPoolConfiguration poolConf = new GoogleSheetv4DataPoolConfiguration();
		poolConf.setFileId(new DynamicValue<String>("1w2YeLORbeMPrnXAIKCRNi8QVnibhwsAk5FatAMFVxRA"));
		poolConf.setServiceAccountKey(new DynamicValue<String>("/../...json"));
		poolConf.setTabName(new DynamicValue<String>("Tabellenblatt1"));

		DataSet<?> pool = DataPoolFactory.getDataPool("gsheet", poolConf, null);

		pool.init();
		pool.next();
		DataPoolRow row = pool.next();
		pool.close();

		Assert.assertEquals("c1", ((Map)row.getValue()).get("column3"));
	}


}
