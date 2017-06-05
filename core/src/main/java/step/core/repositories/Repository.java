package step.core.repositories;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface Repository {

	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception;

	public ImportResult importArtefact(Map<String, String> repositoryParameters) throws Exception;

	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception;

	public void exportExecution(Map<String, String> repositoryParameters, String executionID) throws Exception;
	
	public class ImportResult implements Serializable {
		
		private static final long serialVersionUID = 3711110316457339962L;

		boolean successful = false;;
		
		String artefactId;
		
		List<String> errors;

		public boolean isSuccessful() {
			return successful;
		}

		public void setSuccessful(boolean successful) {
			this.successful = successful;
		}

		public String getArtefactId() {
			return artefactId;
		}

		public void setArtefactId(String artefactId) {
			this.artefactId = artefactId;
		}

		public List<String> getErrors() {
			return errors;
		}

		public void setErrors(List<String> errors) {
			this.errors = errors;
		} 
		
	}

}