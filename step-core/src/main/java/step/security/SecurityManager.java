package step.security;

import java.util.Arrays;

import step.expressions.ExpressionHandler;

public class SecurityManager {

	public static void assertNotInExpressionHandler() {
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		boolean inExpressionHandler = Arrays.asList(stackTrace).stream()
				.filter(e -> e.getClassName().equals(ExpressionHandler.class.getName())).count() > 0;
		if(inExpressionHandler) {
			throw new SecurityException("This method cannot be called within custom expressions");
		}
	}
}
