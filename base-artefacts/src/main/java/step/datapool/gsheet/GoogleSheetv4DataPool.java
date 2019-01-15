/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.datapool.gsheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

/**
 * @author doriancransac
 * @author Jérôme Comte
 *
 */
public class GoogleSheetv4DataPool extends DataSet<GoogleSheetv4DataPoolConfiguration> {

	private String saKey;
	private String fileId;
	private String tabName;

	List<List<Object>> datapool;
	
	List<String> headers;
	
	int cursor;

	/**
	 * @param configuration
	 */
	public GoogleSheetv4DataPool(GoogleSheetv4DataPoolConfiguration configuration) {
		super(configuration);
		saKey = configuration.getServiceAccountKey().get();
		fileId = configuration.getFileId().get();
		tabName = configuration.getTabName().get();
	}

	Sheets service = null;

	@Override
	public void init() {
		createDatapool(saKey, fileId, tabName);
		createHeaders();
		this.cursor = 1; // skip headers
	}

	private void createHeaders() {
		this.headers = new LinkedList<>();
		for(Object o : datapool.get(0)) // 0 = headers row
			this.headers.add((String)o);
	}

	private int getIndexForHeader(String header) {
		return this.headers.indexOf(header);
	}
	
	@Override
	public void reset() {
		init();
	}

	@Override
	public void close() {
		//nothing to do, apparently the Sheet service doesn't need to be closed explicitely
	}

	@Override
	public Object next_() {
		if(cursor >= this.datapool.size())
			return null;
		RowWrapper row = new RowWrapper(cursor);
		cursor++;
		return row;
	}

	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
	
	private class RowWrapper extends SimpleStringMap{
		
		private int cursor;
		
		public RowWrapper(int cursor){
			this.cursor = cursor;
		}
		
		@Override
		public int size() {
			return datapool.get(cursor).size();
		}

		@Override
		public boolean isEmpty() {
			return datapool.get(cursor).isEmpty();
		}

		//TODO: handle other types?
		@Override
		public String get(String key) {
			if(getIndexForHeader(key) < 0)
				throw new RuntimeException("Column unknown:" + key);
			return (String) datapool.get(cursor).get(getIndexForHeader(key));
		}

		
		//TODO:easy to implement by turning the datapool into List<LinkedList<Object>>
		@Override
		public String put(String key, String value) {
			throw new RuntimeException("Not implemented.");
		}

		@Override
		public Set<String> keySet() {
			return new HashSet<>(headers);
		}
		
	}

	public void createDatapool(String saKey, String fileId, String tabName){
		try {
			this.service = buildService(getCredential(saKey));
			this.datapool = getValuesForRange(fileId, tabName);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GoogleCredential getCredential(String saKey) throws Exception {
		Set<String> all = new HashSet<>();
		all.addAll(SheetsScopes.all());
		all.addAll(DriveScopes.all());
		return GoogleCredential.fromStream(new FileInputStream(saKey))
				.createScoped(all);
	}

	public Sheets buildService(GoogleCredential credential) throws IOException,
	GeneralSecurityException {
		return new Sheets.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance(),
				credential)
				.setApplicationName("Sheets API Snippets")
				.build();
	}

	public List<List<Object>> getValuesForRange(String spreadsheetId, String range) throws IOException {
		ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();
		return result.getValues();
	}
}
