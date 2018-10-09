package step.grid;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.SimpleAffinityEvaluator;

public class AffinityEvaluatorImpl extends SimpleAffinityEvaluator<Identity, TokenWrapper> {

	public int getAffinityScore(Identity i1, TokenWrapper i2) {
		if(i2.getTokenHealth()!=null&&i2.getTokenHealth().isHasError()) {
			return -1;
		} else {
			return super.getAffinityScore(i1, i2);
		}
	}
}
