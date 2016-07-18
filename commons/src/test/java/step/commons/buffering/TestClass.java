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
