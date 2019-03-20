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
package step.commons.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FileRepository<T> {
	
	private File configFile;
	
	private TypeReference<T> typeRef;
	
	private Class<T> objectClass;
	
	private FileRepositoryCallback<T> callback;
	
	private static Logger logger = LoggerFactory.getLogger(FileRepository.class);
	
	public FileRepository(File file, Class<T> objectClass, FileRepositoryCallback<T> callback) {
		super();
		this.objectClass = objectClass;
		this.callback = callback;
		this.configFile = file;
		
		init();
	}
	
	public FileRepository(File file, TypeReference<T> typeRef, FileRepositoryCallback<T> callback) {
		super();
		this.typeRef = typeRef;
		this.callback = callback;
		this.configFile = file;

		init();
	}

	public interface FileRepositoryCallback<T> {
		
		public void onLoad(T object) throws Exception;
	}
	
	public void init() {
		loadConfigAndCallback();
		
		FileWatchService.getInstance().register(configFile, new Runnable() {
			@Override
			public void run() {
				loadConfigAndCallback();
			}
		});					
	}
	
	public void destroy() {
		FileWatchService.getInstance().unregister(configFile);
	}
	
	private void loadConfigAndCallback() {
		try {
			T object = parseConfig();
			callback.onLoad(object);
		} catch (Exception e) {
			logger.error("Error while loading loading configuration file '"+configFile.getAbsolutePath()+"'", e);
		}
	}
	
	private T parseConfig() throws IOException {
		InputStream stream = new FileInputStream(configFile);
		try {
			ObjectMapper mapper = new ObjectMapper();
			if(objectClass!=null) {
				return mapper.readValue(stream, objectClass);
			} else {
				return mapper.readValue(stream, typeRef);
			}
		} finally {
			stream.close();
		}
	}
	
	
	public void save(T config) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(configFile, config);
	}
	
}
