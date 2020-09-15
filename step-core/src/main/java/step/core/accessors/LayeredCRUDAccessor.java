package step.core.accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

import com.google.common.collect.Streams;

public class LayeredCRUDAccessor<T extends AbstractIdentifiableObject> implements CRUDAccessor<T> {

	private final List<CRUDAccessor<T>> accessors = new ArrayList<>();
	
	public LayeredCRUDAccessor() {
		super();
	}
	
	public LayeredCRUDAccessor(List<? extends CRUDAccessor<T>> accessors) {
		super();
		this.accessors.addAll(accessors);
	}

	public void addAccessor(CRUDAccessor<T> accessor) {
		accessors.add(accessor);
	}

	public void pushAccessor(CRUDAccessor<T> accessor) {
		accessors.add(0, accessor);
	}

	@Override
	public T get(ObjectId id) {
		return layeredLookup(a->a.get(id));
	}

	protected <V> V layeredLookup(Function<CRUDAccessor<T>, V> f) {
		for (CRUDAccessor<T> crudAccessor : accessors) {
			V result = f.apply(crudAccessor);
			if(result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public T get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		return layeredLookup(a->a.findByAttributes(attributes));
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		return layeredMerge(a->a.findManyByAttributes(attributes));
	}

	protected <V> Spliterator<V> layeredMerge(Function<CRUDAccessor<T>, Spliterator<V>> f) {
		List<V> result = new ArrayList<>();
		accessors.forEach(a->{
			f.apply(a).forEachRemaining(result::add);	
		});
		return result.spliterator();
	}
	
	@Override
	public Iterator<T> getAll() {
		Iterator<CRUDAccessor<T>> accessorIterator = accessors.iterator();
		return new MergeIterator<T>(accessorIterator);
	}

	private static final class MergeIterator<T extends AbstractIdentifiableObject> implements Iterator<T> {
		
		private final Iterator<CRUDAccessor<T>> accessorIterator;
		private Iterator<T> currentIterator;

		private MergeIterator(Iterator<CRUDAccessor<T>> accessorIterator) {
			this.accessorIterator = accessorIterator;
			nextAccessor();
		}

		@Override
		public boolean hasNext() {
			if(currentIterator != null) {
				boolean hasNext = currentIterator.hasNext();
				if(hasNext) {
					return true;
				} else {
					nextAccessor();
					return hasNext();
				}
			} else {
				return false;
			}
		}

		protected void nextAccessor() {
			if(accessorIterator.hasNext()) {
				CRUDAccessor<T> currentAccessor = accessorIterator.next();
				currentIterator = currentAccessor.getAll();
			} else {
				currentIterator = null;
			}
		}

		@Override
		public T next() {
			if(hasNext()) {
				return currentIterator.next();
			} else {
				throw new NoSuchElementException();
			}
		}
	}
	
	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return layeredLookup(a->a.findByAttributes(attributes, attributesMapKey));
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return layeredMerge(a->a.findManyByAttributes(attributes, attributesMapKey));
	}

	@Override
	public List<T> getRange(int skip, int limit) {
		List<T> all = Streams.stream(getAll()).collect(Collectors.toList());
		return all.subList(skip, Math.min(all.size(), skip+limit));
	}

	@Override
	public void remove(ObjectId id) {
		for (CRUDAccessor<T> crudAccessor : accessors) {
			T e = crudAccessor.get(id);
			if(e!= null) {
				crudAccessor.remove(id);
			}
		}
	}

	@Override
	public T save(T entity) {
		return getAccessorForPersistence().save(entity);
	}

	@Override
	public void save(Collection<? extends T> entities) {
		getAccessorForPersistence().save(entities);
	}

	protected CRUDAccessor<T> getAccessorForPersistence() {
		return accessors.get(0);
	}
}
