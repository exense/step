/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.commons.buffering;

import org.junit.Test;

public class TestClass {

	@Test
	public void test() {
		for(char c=0;c<Character.MAX_VALUE;c++){
			byte r = (byte)(192 | c >>> 6 & 31);
			byte r2 =(byte)(128 | c >>> 0 & 63);
			if(r2==-110 && r==-63) {
				System.out.print(c+",");
			}
	    }
	}
}
