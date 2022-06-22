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
package step.plugins.quotamanager;

import java.io.StringWriter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.deployment.AbstractStepServices;
import step.plugins.quotamanager.QuotaHandlerStatus.QuotaHandlerStatusEntry;

@Path("/quotamanager")
@Tag(name = "Quota manager")
public class QuotaManagerServices extends AbstractStepServices {

	@GET
	@Path("/status")
	@Produces(MediaType.TEXT_PLAIN)
	public String getQuotaManagerStatus() {
		StringWriter writer = new StringWriter();
		QuotaManager quotaManager = getContext().get(QuotaManager.class);

		if(quotaManager!=null) {
			writer.write("QuotaManager status:\n");
			for(QuotaHandlerStatus status:quotaManager.getStatus()) {
				writer.write("  Quota \"" + status.getConfiguration().getId() + "\" (" + status.getConfiguration().getDescription() + "):\n");
				for(QuotaHandlerStatusEntry quotaKeyStatus:status.getEntries()) {
					String peakDisplay = quotaKeyStatus.getUsage()>quotaKeyStatus.getPeak()?"n.a.":Integer.toString(quotaKeyStatus.getPeak());
					writer.write("    Key \"" + quotaKeyStatus.getQuotaKey() + "\". Quota usage: " + quotaKeyStatus.getUsage() + "/" + 
							status.getConfiguration().getPermits() + " (Peak: " + peakDisplay + ")" + "\n");
				}
				
			}			
		} else {
			writer.write("The quota manager is disabled. You can enable it by setting the property quotamanager.config in the step configuration.");
		}
		
		return writer.toString();
	}
}
