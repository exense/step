package step.core.miscellaneous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class TestArtefactResultHandler {
	
	static Logger logger = LoggerFactory.getLogger(TestArtefactResultHandler.class);
	
	public static ReportNode failWithException(ReportNode result, Throwable e) {
		return failWithException(result, e, true);
	}
	
	public static ReportNode failWithException(ReportNode result, Throwable e, boolean generateAttachment) {
		return failWithException(result, null, e, generateAttachment);

	}
	
	public static ReportNode failWithException(ReportNode result, String errorMsg, Throwable e, boolean generateAttachment) {
		if(generateAttachment && !(e instanceof ValidationException)) {			
			ReportNodeAttachmentManager.attach(e, result);
		}
		result.addError(errorMsg!=null?errorMsg+":"+e.getMessage():e.getMessage());
		result.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		
		return result;

	}
	
	

}
