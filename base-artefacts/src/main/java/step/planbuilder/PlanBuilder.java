package step.planbuilder;

import java.io.File;
import java.util.Collection;
import java.util.Stack;

import step.artefacts.ForBlock;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.datapool.excel.ExcelDataPool;
import step.datapool.sequence.IntSequenceDataPool;

public class PlanBuilder {

	protected AbstractArtefact root;

	protected InMemoryArtefactAccessor localAccessor = new InMemoryArtefactAccessor();
	
	protected Stack<AbstractArtefact> stack = new Stack<>();
	
	public static PlanBuilder create() {
		return new PlanBuilder();
	}
	
	@SuppressWarnings("unchecked")
	public Plan build() {
		return new Plan(root, (Collection<AbstractArtefact>) localAccessor.getCollection());
	}
	
	public PlanBuilder add(AbstractArtefact artefact) {
		if(root==null) {
			startBlock(sequence());
		}
		localAccessor.save(artefact);
		addToCurrentParent(artefact);
		return this;
	}
	
	public PlanBuilder startBlock(AbstractArtefact a) {
		if(root!=null) {
			addToCurrentParent(a);
			localAccessor.save(a);
		} else {
			root = a;
		}
		stack.push(a);
		return this;
	}
	
	public PlanBuilder endBlock() {
		stack.pop();
		return this;
	}

	private void addToCurrentParent(AbstractArtefact artefact) {
		AbstractArtefact parent = stack.peek();
		localAccessor.get(parent.getId()).addChild(artefact.getId());
	}
	
	public static Sequence sequence() {
		return new Sequence();
	}
	
	public static ForBlock for_(int start, int end) {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(start));;
		conf.setEnd(new DynamicValue<Integer>(end));;
		f.setDataSource(conf);
		return f;
	}
	
	public static ForEachBlock forEachRowInExcel(File file) {
		ForEachBlock f = new ForEachBlock();
		ExcelDataPool p = new ExcelDataPool();
		p.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		p.getHeaders().setValue(true);
		f.setDataSource(p);
		f.setDataSourceType("excel");
		return f;
	}

}
