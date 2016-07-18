package step.commons.pools.selectionpool;

public interface AffinityEvaluator {

	public int getAffinityScore(Identity i1, Identity i2);
}
