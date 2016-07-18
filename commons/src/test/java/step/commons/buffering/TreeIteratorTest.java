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
