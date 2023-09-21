/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.artefacts.handlers;

import step.artefacts.FunctionGroup;
import step.core.AbstractContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.functions.FunctionGroupHandle;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> implements FunctionGroupHandle {
	
	public static final String FUNCTION_GROUP_CONTEXT_KEY = "##functionGroupContext##";

	private TokenSelectorHelper tokenSelectorHelper;	
	
	public FunctionGroupHandler() {
		super();
	}

	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		tokenSelectorHelper = new TokenSelectorHelper(new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler())));

	}

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	public static class FunctionGroupContext {
		
		final List<TokenWrapper> tokens = new ArrayList<>();
		
		TokenWrapper localToken;
		
		final Map<String, Interest> additionalSelectionCriteria;

		final Optional<String> dockerImage;

		final Optional<String> containerUser;

		final Optional<String> containerCommand;
		
		private long ownerThreadId = 0;

		public FunctionGroupContext(Map<String, Interest> additionalSelectionCriteria) {
			this(additionalSelectionCriteria, Optional.empty(), Optional.empty(), Optional.empty());
		}

		public FunctionGroupContext(Map<String, Interest> additionalSelectionCriteria, Optional<String> dockerImage, Optional<String> containerUser, Optional<String> containerCommand) {
			super();
			this.additionalSelectionCriteria = additionalSelectionCriteria;
			this.dockerImage = dockerImage;
			this.containerUser = containerUser;
			this.containerCommand = containerCommand;
		}
		
		public List<TokenWrapper> getTokens() {
			return tokens;
		}

		public boolean addToken(TokenWrapper e) {
			return tokens.add(e);
		}

		public TokenWrapper getLocalToken() {
			return localToken;
		}

		public void setLocalToken(TokenWrapper localToken) {
			this.localToken = localToken;
		}

		public Map<String, Interest> getAdditionalSelectionCriteria() {
			return additionalSelectionCriteria;
		}

		public boolean isOwner(long id) {
			if (ownerThreadId == 0) {
				ownerThreadId = id;
				return true;
			} else {
				return (ownerThreadId == id);
			}
		}
		
	}
	
	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) throws Exception {		
		Map<String, Interest> additionalSelectionCriteria = tokenSelectorHelper.getTokenSelectionCriteria(testArtefact, getBindings());
		String dockerImage = testArtefact.getDockerImage().get();
		String containerUser = testArtefact.getContainerUser().get();
		String containerCommand = testArtefact.getContainerCommand().get();

		Optional<String> dockerImageOptional;
		if(dockerImage != null && !dockerImage.isEmpty()) {
			dockerImageOptional = Optional.ofNullable(dockerImage);
		} else {
			dockerImageOptional = Optional.empty();
		}

		Optional<String> containerUserOptional = containerUser != null && !containerUser.isEmpty() ? Optional.ofNullable(containerUser) : Optional.empty();
		Optional<String> containerCommandOptional = containerCommand != null && !containerCommand.isEmpty() ? Optional.ofNullable(containerCommand) : Optional.empty();


		// TODO switch this to a required criteria
		dockerImageOptional.ifPresent(image -> additionalSelectionCriteria.put("$docker", new Interest(Pattern.compile("true"), false)));

		FunctionGroupContext handle = new FunctionGroupContext(additionalSelectionCriteria, dockerImageOptional,  containerUserOptional, containerCommandOptional);
		context.getVariablesManager().putVariable(node, FUNCTION_GROUP_CONTEXT_KEY, handle);
		context.put(FunctionGroupHandle.class, this);
		try {
			BiConsumer<AbstractArtefact, ReportNode> consumer = testArtefact.getConsumer();
			if(consumer == null) {
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
				scheduler.execute_(node, testArtefact);
			} else {
				consumer.accept(testArtefact, node);
			}
		} finally {
			releaseTokens(context, true);
		}	
	}
	
	@Override
	public void releaseTokens(AbstractContext context, boolean local) throws Exception {
		FunctionGroupContext handle = (FunctionGroupContext) ((ExecutionContext) context).getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
		FunctionExecutionService functionExecutionService = context.get(FunctionExecutionService.class);
		List<Exception> releaseExceptions = new ArrayList<>();
		if(handle.getTokens()!=null) {
			handle.getTokens().forEach(t->{
				try {
					functionExecutionService.returnTokenHandle(t.getID());
				} catch (FunctionExecutionServiceException e) {
					releaseExceptions.add(e);
				}
			});
			handle.getTokens().clear();
		}
		if(handle.getLocalToken()!=null && local) {
			try {
				functionExecutionService.returnTokenHandle(handle.getLocalToken().getID());
			} catch (FunctionExecutionServiceException e) {
				releaseExceptions.add(e);
			} finally {
				handle.setLocalToken(null);
			}
		}
		
		int exceptionCount = releaseExceptions.size();
		if(exceptionCount > 0) {
			if(exceptionCount == 1) {
				throw releaseExceptions.get(0);
			} else {
				throw new Exception("Multiple errors occurred when releasing agent tokens: "+
							releaseExceptions.stream().map(e->e.getMessage()).collect(Collectors.joining(", ")));
			}
		}
	
		
	}
	
	@Override
	public boolean isInSession(AbstractContext context) {
		return ((ExecutionContext) context).getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY) != null;
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
