package step.repositories.staging;

import java.util.Map;

import step.core.artefacts.ArtefactAccessor;
import step.core.execution.ExecutionContext;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.Repository;
import step.core.repositories.TestSetStatusOverview;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;

public class StagingRepository implements Repository {

	protected StagingContextAccessorImpl stagingContextAccessor;
	
	public StagingRepository(StagingContextAccessorImpl stagingContextRegistry) {
		super();
		this.stagingContextAccessor = stagingContextRegistry;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
		StagingContext stagingContext = stagingContextAccessor.get(repositoryParameters.get("contextid"));
		ArtefactInfo info = new ArtefactInfo();
		info.setType("testplan");
		info.setName(stagingContext.getPlan().getRoot().getAttributes().get("name"));
		return info;
	}

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		StagingContext stagingContext = stagingContextAccessor.get(repositoryParameters.get("contextid"));
		
		ArtefactAccessor artefactAccessor = context.getArtefactAccessor();
		stagingContext.plan.getArtefacts().forEach(a->artefactAccessor.save(a));
		
		ImportResult result = new ImportResult();
		result.setArtefactId(stagingContext.plan.getRoot().getId().toString());
		
		stagingContext.plan.getFunctions().iterator().forEachRemaining(f->((FunctionCRUDAccessor)context.get(FunctionAccessor.class)).save(f));
		
		result.setSuccessful(true);
	
		return result;
	}

	@Override
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
