package step.grid.tokenpool;

import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAffinityEvaluator implements AffinityEvaluator {

	private final static Logger logger = LoggerFactory.getLogger(SimpleAffinityEvaluator.class);
	
	@Override
	public int getAffinityScore(Identity i1, Identity i2) {
		
		int pretenderScore = getScore(i1, i2);
		if(pretenderScore!=-1) {
			int tokenScore = getScore(i2, i1);
			if(tokenScore!=-1) {
				log(i1, i2, pretenderScore, tokenScore);
				return pretenderScore+tokenScore;
			} else {
				log(i1, i2, pretenderScore, tokenScore);
				return -1;
			}
		} else {
			log(i1, i2, pretenderScore, 0);
			return -1;
		}
	}
	
	private void log(Identity i1, Identity i2, int pretenderScore, int tokenScore) {
		if(logger.isDebugEnabled())
			logger.debug("Calculated affinity between identity 1 ("+i1.toString() +") and identity 2 (" +
				i2.toString() + "). TokenScore: " + tokenScore + ". PretenderScore: " + pretenderScore);
	}
	
	private int getScore(Identity pretender, Identity queen) {
		int score = 0;
		if(queen.getInterests()!=null) {
			for(String criterionAttribute:queen.getInterests().keySet()) {
				String attribute = pretender.getAttributes().get(criterionAttribute);
				Interest interest = queen.getInterests().get(criterionAttribute);
				if(attribute!=null) {
					Matcher matcher = interest.getSelectionPattern().matcher(attribute);
					if(matcher.matches()) {
						score++;
					} else {
						if(interest.isMust()) {
							return -1;
						}
					}
				} else {
					if(interest.isMust()) {
						return -1;
					}
				}
			}
		}
		return score;
	}

}
