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
		QuotaManager quotaManager = (QuotaManager) getContext().get(QuotaManagerPlugin.QUOTAMANAGER_KEY);

		writer.write("QuotaManager status:\n");
		for(QuotaHandlerStatus status:quotaManager.getStatus()) {
			writer.write("  Quota \"" + status.getConfiguration().getId() + "\" (" + status.getConfiguration().getDescription() + "):\n");
			for(QuotaHandlerStatusEntry quotaKeyStatus:status.getEntries()) {
				String peakDisplay = quotaKeyStatus.getUsage()>quotaKeyStatus.getPeak()?"n.a.":Integer.toString(quotaKeyStatus.getPeak());
				writer.write("    Key \"" + quotaKeyStatus.getQuotaKey() + "\". Quota usage: " + quotaKeyStatus.getUsage() + "/" + 
					status.getConfiguration().getPermits() + " (Peak: " + peakDisplay + ")" + "\n");
			}
			
		}
		
		return writer.toString();
	}
}
