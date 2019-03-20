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
