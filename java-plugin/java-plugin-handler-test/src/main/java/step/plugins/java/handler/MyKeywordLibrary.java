package step.plugins.java.handler;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class MyKeywordLibrary extends AbstractKeyword {

	@Keyword
	public void MyKeyword1() {
		output.add("MyKey", "MyValue");
	}
}
