package step.commons.buffering;

import java.util.Iterator;

public class TreeIterator<T> implements Iterator<T> {

	final Iterator<T> it;
	
	final TreeIteratorFactory<T> factory; 

	Iterator<T> childrenIt = null;

	public TreeIterator(Iterator<T> it, TreeIteratorFactory<T> factory) {
		super();
		this.it = it;
		this.factory = factory;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext() || (childrenIt != null && childrenIt.hasNext());
	}

	@Override
	public T next() {
		if (childrenIt == null || !childrenIt.hasNext()) {
			T nextParent = it.next();
			childrenIt = new TreeIterator<T>(factory.getChildrenIterator(nextParent), factory);
			return nextParent;
		} else {
			return childrenIt.next();
		}
	}

	@Override
	public void remove() {
		throw new RuntimeException("Remove method not supported!");
	}
	
}
