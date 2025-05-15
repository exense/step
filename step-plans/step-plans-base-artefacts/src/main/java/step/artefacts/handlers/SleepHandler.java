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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DurationFormatUtils;

import step.artefacts.Sleep;
import step.artefacts.reports.SleepReportNode;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.CancellableSleep;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class SleepHandler extends ArtefactHandler<Sleep, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Sleep testArtefact) {

	}
	
	protected long getValueAsLong(Object value) {
		long sleepDurationMs;
		if (value instanceof java.lang.Long || value instanceof java.lang.Integer) {
			sleepDurationMs = ((Number)value).longValue();
		} else if (value instanceof java.lang.String) {
			sleepDurationMs = Long.parseLong((String) value);
		} else {
			throw new RuntimeException("Unable to parse attribute 'ms' as long.");
		}
		return sleepDurationMs;
	}

	@Override
	protected void execute_(ReportNode node, Sleep testArtefact) {
		boolean releaseToken = testArtefact.getReleaseTokens().get();
		boolean inSession = isInSession();
		if (releaseToken && inSession) {
			releaseTokens();
		}
		long sleepDurationMs;
		try {
			sleepDurationMs = getValueAsLong(testArtefact.getDuration().get());
			String unit = testArtefact.getUnit().get();
			if (unit.equals("s")) {
				sleepDurationMs*=1000;
			} else if (unit.equals("m")) {
				sleepDurationMs*=60000;
			} else if (!unit.equals("ms")) {
				throw new RuntimeException("Supported units are 'ms', 's' and 'm', respectively for milliseconds, seconds and minutes. Provided was " + unit);
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Unable to parse attribute 'ms' as long.",e);
		}
		
		Map<String,String> details = new LinkedHashMap<>();
		details.put("Sleep time", DurationFormatUtils.formatDuration(sleepDurationMs, "HH:mm:ss.SSS"));
		if (inSession) {
			details.put("Release token", Boolean.toString(releaseToken));
		}
		OperationManager.getInstance().enter("Sleep", details, node.getId().toString(), node.getArtefactHash());

		ReportNodeStatus finalStatus = ReportNodeStatus.PASSED;
		if (!context.isSimulation()) {
			if (!CancellableSleep.sleep(sleepDurationMs, context::isInterrupted, SleepHandler.class)) {
				finalStatus = ReportNodeStatus.INTERRUPTED;
			}
		}
		node.setStatus(finalStatus);

		OperationManager.getInstance().exit();
	}

	@Override
	public SleepReportNode createReportNode_(ReportNode parentNode, Sleep testArtefact) {
		return new SleepReportNode();
	}
}
