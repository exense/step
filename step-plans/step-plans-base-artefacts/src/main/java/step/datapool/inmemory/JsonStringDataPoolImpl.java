/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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

package step.datapool.inmemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.datapool.DataSet;

@SuppressWarnings({"unchecked", "rawtypes"})
public class JsonStringDataPoolImpl extends DataSet<JsonStringDataPoolConfiguration> {

	private static Logger logger = LoggerFactory.getLogger(JsonStringDataPoolImpl.class);

	Map<String, Object> map;

	int cursor = 0;

	public JsonStringDataPoolImpl(JsonStringDataPoolConfiguration configuration){
		super(configuration);

		try {
			map = new ObjectMapper().readValue(configuration.getJson().get(), HashMap.class);
		} catch (Exception e) {
			logger.error("Couldn't parse json string.", e);
			throw new RuntimeException("Couldn't parse json string. Original exception=" + e.getMessage());
		}
	}

	@Override
	public void reset() {
		cursor = 0;
	}

	@Override
	public Object next_(){
		Map<String, String> row = new HashMap<String, String>();

		Set<String> set = map.keySet();

		try {
			for(String s : set)
			{
				List values = (List)map.get(s);
				if(cursor >= values.size())
					return null;
				row.put(s, (String) values.get(cursor));
			}
		} catch (Exception e) {
			logger.error("Incorrect map content. Please follow the pattern { \"a\" : [\"va1\", \"va2\", \"va3\"], \"b\" : [\"vb1\", \"vb2\", \"vb3\"] }", e);
			throw new RuntimeException("Incorrect map content. Please follow the pattern { \"a\" : [\"va1\", \"va2\", \"va3\"], \"b\" : [\"vb1\", \"vb2\", \"vb3\"] }. Original exception=" + e.getMessage());
		}
		cursor++;
		return row.size()>0?row:null;
	}

	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}
