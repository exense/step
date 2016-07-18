package step.core.artefacts;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

@Retention(RetentionPolicy.RUNTIME)
public @interface Artefact {

	String name() default ""; 

	Class<? extends ArtefactHandler<?,?>> handler();
	
	Class<? extends ReportNode> report() default ReportNode.class;
	
	boolean block() default true;
	
}
