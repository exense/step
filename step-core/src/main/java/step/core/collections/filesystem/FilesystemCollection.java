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
package step.core.collections.filesystem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.beanutils.PropertyUtils;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.PojoFilter;
import step.core.collections.PojoFilters.PojoFilterFactory;
import step.core.collections.SearchOrder;

public class FilesystemCollection<T extends AbstractIdentifiableObject> implements Collection<T> {

	private static final String FILE_EXTENSION = ".entity";
	private final ObjectMapper mapper;
	private final File repository;
	private final Class<T> entityClass;

	public FilesystemCollection(File repository, Class<T> entityClass) {
		super();
		this.repository = repository;
		this.entityClass = entityClass;
		this.mapper = DefaultJacksonMapperProvider.getObjectMapper(new YAMLFactory());
		if (!repository.exists()) {
			repository.mkdirs();
		}
	}

	@Override
	public List<String> distinct(String columnName, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> distinct(String columnName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static class FileAndEntity<T> {
		
		private final File file;
		private final T entity;
		
		public FileAndEntity(File file, T entity) {
			super();
			this.file = file;
			this.entity = entity;
		}

		public File getFile() {
			return file;
		}

		public T getEntity() {
			return entity;
		}
	}
	
	private Stream<FileAndEntity<T>> entityStream() {
		return Arrays.asList(repository.listFiles(f -> f.getName().endsWith(FILE_EXTENSION))).stream().map(f->new FileAndEntity<>(f, readFile(f)));
	}
	
	@Override
	public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		Stream<T> stream = filteredStream(filter).map(FileAndEntity::getEntity).sorted(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		if(skip != null) {
			stream = stream.skip(skip);
		}
		if(limit != null) {
			stream = stream.limit(limit);
		}
		if(order != null) {
			Comparator<T> comparing = Comparator.comparing(e->{
				try {
					return PropertyUtils.getProperty(e, order.getAttributeName()).toString();
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
					throw new RuntimeException(e1);
				}
			});
			if(order.getOrder()<0) {
				comparing = comparing.reversed();
			}
			stream = stream.sorted(comparing);
		}
		return stream;
	}

	private Stream<FileAndEntity<T>> filteredStream(Filter filter) {
		PojoFilter<T> pojoFilter = new PojoFilterFactory<T>().buildFilter(filter);
		Iterator<FileAndEntity<T>> it = entityStream().iterator();
		Spliterator<FileAndEntity<T>> spliterator = Spliterators.spliteratorUnknownSize(it, 0);
		Stream<FileAndEntity<T>> filter2 = StreamSupport.stream(spliterator, false).filter(f->pojoFilter.test(f.entity));
		return filter2;
	}

	@Override
	public void remove(Filter filter) {
		filteredStream(filter).forEach(f->{
			f.getFile().delete();
		});
	}

	@Override
	public T save(T entity) {
		if (entity.getId() == null) {
			entity.setId(new ObjectId());
		}
		File file = getFile(entity);
		writeEntity(entity, file);
		return entity;
	}

	@Override
	public void save(Iterable<T> entities) {
		if (entities != null) {
			entities.forEach(e -> save(e));
		}
	}
	
	private T readFile(File file) {
		try {
			return mapper.readValue(file, entityClass);
		} catch (IOException e) {
			throw new FilesystemCollectionException(e);
		}
	}
	
	private void writeEntity(T entity, File file) {
		try {
			mapper.writeValue(file, entity);
		} catch (IOException e) {
			throw new FilesystemCollectionException(e);
		}
	}

	private File getFile(T entity) {
		ObjectId id = entity.getId();
		File file = getFileById(id);
		return file;
	}

	private File getFileById(ObjectId id) {
		String filename = id.toString() + FILE_EXTENSION;
		File file = new File(repository.getAbsolutePath() + "/" + filename);
		return file;
	}

	@Override
	public void createOrUpdateIndex(String field) {
		// not supported
	}

	@Override
	public void createOrUpdateCompoundIndex(String... fields) {
		// not supported
	}
}
