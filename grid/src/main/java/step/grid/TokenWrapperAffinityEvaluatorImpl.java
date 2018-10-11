package step.grid;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.SimpleAffinityEvaluator;

public class TokenWrapperAffinityEvaluatorImpl extends SimpleAffinityEvaluator<Identity, TokenWrapper> {

	public int getAffinityScore(Identity i1, TokenWrapper i2) {
		if(i2.getState().equals(TokenWrapperState.ERROR)||i2.getState().equals(TokenWrapperState.MAINTENANCE)) {
			return -1;
		} else {
			return super.getAffinityScore(i1, i2);
		}
	}
}
