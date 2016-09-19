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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TreeIteratorTest {
	
	class TreeNode {
		List<TreeNode> children = new ArrayList<TreeNode>();
		
		String name;

		public TreeNode(String name) {
			super();
			this.name = name;
		}

		public boolean add(TreeNode e) {
			return children.add(e);
		}

		@Override
		public String toString() {
			return "TreeNode [name=" + name + "]";
		}
	}
	
	@Test
	public void test() {
		TreeIteratorFactory<TreeNode> f = getFactory();
		
		TreeNode l1_1 = new TreeNode("l1_1");
		TreeNode l2_1 = new TreeNode("l2_1");
		TreeNode l2_2 = new TreeNode("l2_2");
		TreeNode l3_1 = new TreeNode("l3_1");
		
		l1_1.add(l2_1);
		l1_1.add(l2_2);
		
		l2_1.add(l3_1);
		
		List<TreeNode> rootNodes = new ArrayList<TreeIteratorTest.TreeNode>();
		rootNodes.add(l1_1);
		TreeIterator<TreeNode> i = new TreeIterator<TreeIteratorTest.TreeNode>(rootNodes.iterator(), f);
		
		List<TreeNode> result = new ArrayList<TreeIteratorTest.TreeNode>();
		while(i.hasNext()) {
			result.add(i.next());
		}
		
		Assert.assertEquals(4, result.size());
	}
	
	@Test
	public void testNoChildren() {
		TreeIteratorFactory<TreeNode> f = getFactory();
		
		TreeNode l1_1 = new TreeNode("l1_1");
		
		List<TreeNode> rootNodes = new ArrayList<TreeIteratorTest.TreeNode>();
		rootNodes.add(l1_1);
		TreeIterator<TreeNode> i = new TreeIterator<TreeIteratorTest.TreeNode>(rootNodes.iterator(), f);
		
		List<TreeNode> result = new ArrayList<TreeIteratorTest.TreeNode>();
		while(i.hasNext()) {
			result.add(i.next());
		}
		
		Assert.assertEquals(1, result.size());
	}
	
	private TreeIteratorFactory<TreeNode> getFactory() {
		TreeIteratorFactory<TreeNode> f = new TreeIteratorFactory<TreeIteratorTest.TreeNode>() {
			@Override
			public Iterator<TreeNode> getChildrenIterator(TreeNode parent) {
				return parent.children.iterator();
			}
		};
		return f;
	}
	

}
