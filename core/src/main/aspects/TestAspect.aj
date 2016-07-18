import java.util.Map;


public aspect TestAspect {

	pointcut acquirePermit():
	    call(void QuotaHanlder.acquirePermit(Map));
	
	before(): acquirePermit() {
		OperationManager.getInstance().enter("Quota acquisition AspectJ");
	}
}
