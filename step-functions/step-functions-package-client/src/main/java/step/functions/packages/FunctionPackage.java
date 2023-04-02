package step.functions.packages;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.objectenricher.EnricheableObject;
import step.functions.Function;
import step.resources.Resource;

/**
 * Represents a package of {@link Function}
 *
 */
public class FunctionPackage extends AbstractOrganizableObject implements EnricheableObject {

	public static final String TRACKING_FIELD = "tracking";
	
	protected String packageLibrariesLocation;
	
	protected String packageLocation;

	protected boolean watchForChange;
	
	protected Map<String, String> packageAttributes;
	
	protected boolean executeLocally;
	
	protected Map<String, String> tokenSelectionCriteria;
	
	/**
	 * Keep track of the functions added by this package
	 */	
	protected List<ObjectId> functions;

	/**
	 * @return the resource path to the package libraries. Package libraries are either a folder of jar or DLLs
	 */
	@EntityReference(type=EntityManager.resources)
	public String getPackageLibrariesLocation() {
		return packageLibrariesLocation;
	}

	public void setPackageLibrariesLocation(String packageLibrariesLocation) {
		this.packageLibrariesLocation = packageLibrariesLocation;
	}

	/**
	 * @return the path to the package file. might be a {@link Resource}
	 */
	@EntityReference(type=EntityManager.resources)
	public String getPackageLocation() {
		return packageLocation;
	}

	public void setPackageLocation(String packageLocation) {
		this.packageLocation = packageLocation;
	}

	/**
	 * @return true if changes to the content of the package file have to be tracked to automatically update the package
	 */
	public boolean isWatchForChange() {
		return watchForChange;
	}

	public void setWatchForChange(boolean watchForChange) {
		this.watchForChange = watchForChange;
	}

	/**
	 * @return the additional attributes that have to be added to the attributes of the functions contained in this package
	 */
	public Map<String, String> getPackageAttributes() {
		return packageAttributes;
	}

	public void setPackageAttributes(Map<String, String> packageAttributes) {
		this.packageAttributes = packageAttributes;
	}

	/**
	 * @return the ID of the functions tracked by this package
	 */
	@EntityReference(type="functions")
	public List<ObjectId> getFunctions() {
		return functions;
	}

	public void setFunctions(List<ObjectId> functions) {
		this.functions = functions;
	}

	public boolean isExecuteLocally() {
		return executeLocally;
	}

	public void setExecuteLocally(boolean executeLocally) {
		this.executeLocally = executeLocally;
	}

	public Map<String, String> getTokenSelectionCriteria() {
		return tokenSelectionCriteria;
	}

	public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
		this.tokenSelectionCriteria = tokenSelectionCriteria;
	}

	@Override
	public String toString() {
		return "FunctionPackage [packageLibrariesLocation=" + packageLibrariesLocation + ", packageLocation="
				+ packageLocation + "]";
	}
}
