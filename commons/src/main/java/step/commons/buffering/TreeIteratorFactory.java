package step.commons.buffering;

import java.util.Iterator;

public interface TreeIteratorFactory<T> {

	public Iterator<T> getChildrenIterator(T parent);
	
}
