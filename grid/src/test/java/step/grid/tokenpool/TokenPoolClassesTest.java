package step.grid.tokenpool;
import java.util.regex.Pattern;

import org.junit.Test;

import junit.framework.Assert;


public class TokenPoolClassesTest {

	@Test
	public void testSelectionCriterionEquality() {
		Interest i1 = new Interest(Pattern.compile("QFT.*"), true);
		Interest i2 = new Interest(Pattern.compile("QFT.*"), true);
		
		Assert.assertEquals(i1, i2);;
		Assert.assertEquals(i1.hashCode(), i2.hashCode());;
	}
}
