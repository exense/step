package step.artefacts;

import step.artefacts.handlers.RetryIfFailsHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = RetryIfFailsHandler.class)
public class RetryIfFails extends AbstractArtefact {
	
	@DynamicAttribute
	String maxRetries;
	
	@DynamicAttribute
	String gracePeriod;
	
	public Integer getMaxRetries() {
		return maxRetries!=null&&maxRetries.length()>0?Integer.parseInt(maxRetries):2;
	}
	
	public void setMaxRetries(String maxRetries) {
		this.maxRetries = maxRetries;
	}

	public Integer getGracePeriod() {
		return gracePeriod!=null&&gracePeriod.length()>0?Integer.parseInt(gracePeriod):0;
	}
	
	public void setGracePeriod(String gracePeriod) {
		this.gracePeriod = gracePeriod;
	}

}
