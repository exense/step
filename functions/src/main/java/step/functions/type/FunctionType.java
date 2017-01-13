package step.functions.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionType {

	String name() default ""; 
	
	String label() default "";

}
