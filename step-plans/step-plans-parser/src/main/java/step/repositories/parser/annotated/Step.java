package step.repositories.parser.annotated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Step {

	String value();
	
	int priority() default 1;
}
