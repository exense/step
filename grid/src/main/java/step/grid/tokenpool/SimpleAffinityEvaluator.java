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
package step.grid.tokenpool;

import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAffinityEvaluator<P extends Identity, F extends Identity> implements AffinityEvaluator<P, F> {

	private final static Logger logger = LoggerFactory.getLogger(SimpleAffinityEvaluator.class);
	
	@Override
	public int getAffinityScore(P i1, F i2) {
		
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
