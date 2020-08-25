/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.quotamanager;

import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.plugins.quotamanager.QuotaHandlerStatus.QuotaHandlerStatusEntry;

@Path("/quotamanager")
public class QuotaManagerServices extends AbstractServices {

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
