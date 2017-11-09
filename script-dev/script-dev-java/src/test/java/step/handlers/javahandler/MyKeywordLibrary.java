package step.handlers.javahandler;

public class MyKeywordLibrary extends AbstractKeyword {

	@Keyword
	public void MyKeyword() {
		output.add("test", "test");
	}
}
