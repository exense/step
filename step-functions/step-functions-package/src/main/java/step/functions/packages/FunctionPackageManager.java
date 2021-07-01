package step.functions.packages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.resources.Resource;
import step.resources.ResourceManager;

/**
 * This class is responsible for the handling of {@link FunctionPackage}
 * 
 * It is responsible for the import of {@link FunctionPackage}. 
 * The import of a {@link FunctionPackage} means:
 * <ul>
 * 	<li>the persistence of the {@link FunctionPackage} itself</li>
 *  <li>the definition of the {@link Function} contained in the package</li>
 *  <li>the registration of change watcher of the package file of the {@link FunctionPackage}</li>
 * </ul>
 *
 */
public class FunctionPackageManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackageManager.class);

	private static final String WATCH_FOR_CHANGE = "plugins.FunctionPackagePlugin.watchForChange";
	private static final String WATCHER_INTERVAL = "plugins.FunctionPackagePlugin.watchForChange.interval";
	
	private FunctionPackageAccessor functionPackageAccessor;
	private FunctionManager functionRepository;
	private ResourceManager resourceManager;
	private FileResolver fileResolver;
	private FunctionPackageChangeWatcher changeWatcher;

	private List<FunctionPackageHandler> packageHandlers = new ArrayList<>();

	public FunctionPackageManager(FunctionPackageAccessor functionPackageAccessor,
			FunctionManager functionRepository, ResourceManager resourceManager, FileResolver fileResolver, Configuration configuration) {
		super();
		this.functionPackageAccessor = functionPackageAccessor;
		this.functionRepository = functionRepository;
		this.resourceManager = resourceManager;
		this.fileResolver = fileResolver;
		
		if (configuration.getPropertyAsBoolean(WATCH_FOR_CHANGE,true)) {
			int interval = configuration.getPropertyAsInteger(WATCHER_INTERVAL,60000);
			changeWatcher = new FunctionPackageChangeWatcher(functionPackageAccessor, this, interval);
		}
	}

	public void start() {
		if (changeWatcher!=null) {
			changeWatcher.registerWatchers();
		}
	}

	/**
	 * Registers a {@link FunctionPackageHandler}
	 * 
	 * @param packageHandler the instance of the {@link FunctionPackageHandler}
	 */
	public void registerFunctionPackageHandler(FunctionPackageHandler packageHandler) {
		packageHandlers.add(packageHandler);
	}

	private FunctionPackageHandler getPackageHandler(FunctionPackage functionPackage) throws UnsupportedFunctionPackageType {
		try {
			return packageHandlers.stream().filter(f->f.isValidForPackage(functionPackage)).findFirst().get();
		} catch(NoSuchElementException e) {
			throw new UnsupportedFunctionPackageType("Unsupported package type");
		}
	}

	/**
	 * Reloads a {@link FunctionPackage}.
	 * This triggers a re-import of the {@link Function}s contained in the package
	 * 
	 * @param functionPackageId the ID of the {@link FunctionPackage} to be reloaded
	 * @return the updated {@link FunctionPackage}
	 * @throws Exception if any error occurs during reloading
	 */
	public FunctionPackage reloadFunctionPackage(String functionPackageId, ObjectEnricher objectEnricher) throws Exception {
		assert functionPackageId!=null;
		FunctionPackage functionPackage = functionPackageAccessor.get(new ObjectId(functionPackageId));	
		assert functionPackage!=null;
		return addOrUpdateFunctionPackage(functionPackage, functionPackage, objectEnricher);
	}

	/**
	 * Adds or updates a {@link FunctionPackage}
	 * This triggers the import or re-import of the {@link Function}s contained in the package
	 * 
	 * @param newFunctionPackage the {@link FunctionPackage} to be loaded
	 * @return the updated {@link FunctionPackage}
	 * @throws Exception if any error occurs during loading
	 */
	public FunctionPackage addOrUpdateFunctionPackage(FunctionPackage newFunctionPackage, ObjectEnricher objectEnricher) throws Exception {
		FunctionPackage previousFunctionPackage = null;
		if(newFunctionPackage.getId()!=null) {
			previousFunctionPackage = functionPackageAccessor.get(newFunctionPackage.getId());
			cleanupObsoleteResource(previousFunctionPackage, newFunctionPackage);
		}
		return addOrUpdateFunctionPackage(previousFunctionPackage, newFunctionPackage, objectEnricher);
	}

	/**
	 * Adds or updates a resource-based {@link FunctionPackage}
	 * This variation allows for the implicit identification and overwrite of a keyword package
	 * based on the name of the associated resource
	 * 
	 * @param newFunctionPackage the {@link FunctionPackage} to be loaded
	 * @return the updated {@link FunctionPackage}
	 * @throws Exception if any error occurs during loading
	 */
	public FunctionPackage addOrUpdateResourceBasedFunctionPackage(FunctionPackage newFunctionPackage, ObjectEnricher objectEnricher) throws Exception {
		if(newFunctionPackage.getCustomFields() != null) {
			String resourceName = (String) newFunctionPackage.getCustomField("resourceName");
			if(resourceName != null) {
				FunctionPackage lookedupBasedOnResource = getPackageByResourceName(resourceName);
				cleanupObsoleteResource(lookedupBasedOnResource, newFunctionPackage);
				return addOrUpdateFunctionPackage(lookedupBasedOnResource, newFunctionPackage, objectEnricher);
			}
		}
		return addOrUpdateFunctionPackage(newFunctionPackage, objectEnricher);
	}
	

	private void cleanupObsoleteResource(FunctionPackage previousFunctionPackage, FunctionPackage newFunctionPackage) {
		if(previousFunctionPackage!=null && newFunctionPackage!=null ) {
			//cleanup main resource
			if(previousFunctionPackage.getPackageLocation() != null && newFunctionPackage.getPackageLocation() != null) {
				String previousResourceId = getResourceId(previousFunctionPackage);
				String newResourceId = getResourceId(newFunctionPackage);
				if(previousResourceId != null && !previousResourceId.equals(newResourceId)){
					if (resourceManager.resourceExists(previousResourceId)) {
						resourceManager.deleteResource(previousResourceId);
					}
				}
			}
			
			//cleanup library resource
			if(previousFunctionPackage.getPackageLibrariesLocation() != null && newFunctionPackage.getPackageLibrariesLocation() != null) {
				String previousResourceId = getLibraryResourceId(previousFunctionPackage);
				String newResourceId = getLibraryResourceId(newFunctionPackage);
				if(previousResourceId != null && !previousResourceId.equals(newResourceId)){
					if (resourceManager.resourceExists(previousResourceId)) {
						resourceManager.deleteResource(previousResourceId);
					}
				}
			}
		}
	}

	private List<Function> deleteFunctions(FunctionPackage previousFunctionPackage) {
		List<Function> previousFunctions;
		previousFunctions = getPackageFunctions(previousFunctionPackage);
		previousFunctions.forEach(function->{
			try {
				functionRepository.deleteFunction(function.getId().toString());
			} catch (FunctionTypeException e) {
				logger.error("Error while deleting function "+function.getId().toString(), e);
			}
		});
		return previousFunctions;
	}

	private FunctionPackage addOrUpdateFunctionPackage(FunctionPackage previousFunctionPackage, FunctionPackage newFunctionPackage, ObjectEnricher objectEnricher) throws Exception {
		String packageLocation = newFunctionPackage.getPackageLocation();
		if(packageLocation == null || packageLocation.trim().length() == 0) {
			throw new Exception("Empty package file");
		}

		// Auto detect the appropriate package handler
		FunctionPackageHandler handler = getPackageHandler(newFunctionPackage);

		// set resourceId link as attribute for implicit lookup via resource
		if(newFunctionPackage.getPackageLocation() != null) {
			newFunctionPackage.addCustomField("resourceId", getResourceId(newFunctionPackage));
		}
		
		//Get the reference package's functions
		List<Function> referenceFunctions = new ArrayList<Function>();
		String referenceID = newFunctionPackage.getReferencePackageId();
		if (referenceID!=null && !referenceID.isEmpty()) {
			FunctionPackage reference = functionPackageAccessor.get(new ObjectId(referenceID));
			referenceFunctions = getPackageFunctions(reference);
		}

		List<Function> previousFunctions = null;
		// Remove functions from previous package
		if(previousFunctionPackage!=null) {
			previousFunctions = deleteFunctions(previousFunctionPackage);
			unregisterWatcher(previousFunctionPackage);
		}

		//If we have a previous package with an id, this is an update
		if(previousFunctionPackage!=null && previousFunctionPackage.getId()!=null) {
			//If we're updating an existing package with attributes coming from a new instance of a package
			if(!previousFunctionPackage.getId().toString().equals(newFunctionPackage.getId().toString())) {
				// Then we override the id of the new package with the old id to override the existing entity and avoid object duplicates
				newFunctionPackage.setId(previousFunctionPackage.getId());
			}
		}
		
		// Build the Functions with the appropriate handler
		List<Function> functions = handler.buildFunctions(newFunctionPackage, false);

		List<ObjectId> newFunctionIds = new ArrayList<>();
		
		if (newFunctionPackage.getAttributes()==null) {
			newFunctionPackage.setAttributes(new HashMap<>());
		}
		
		for(Function newFunction:functions) {	
			
			// apply attribute map of package object
			newFunction.getAttributes().putAll(excludeFieldFromMap(newFunctionPackage.getAttributes(), AbstractOrganizableObject.NAME));
			
			// apply the individual attributes of the reference function:
			for (Function ref : referenceFunctions) {
				if (ref.getAttributes().get(AbstractOrganizableObject.NAME).equals(newFunction.getAttributes().get(AbstractOrganizableObject.NAME))) {
					newFunction.setCallTimeout(ref.getCallTimeout());
					newFunction.setSchema(ref.getSchema());
					newFunction.setHtmlTemplate(ref.getHtmlTemplate());
					newFunction.setTokenSelectionCriteria(ref.getTokenSelectionCriteria());
					if(ref.getAttributes() != null) {
						newFunction.getAttributes().putAll(excludeFieldFromMap(ref.getAttributes(), AbstractOrganizableObject.NAME));
					}
					newFunction.setCustomFields(ref.getCustomFields());
					break;
				}
			}
			
			// apply packageAttributes
			if(newFunctionPackage.getPackageAttributes()!=null) {
				newFunction.getAttributes().putAll(excludeFieldFromMap(newFunctionPackage.getPackageAttributes(), AbstractOrganizableObject.NAME));
			}
			
			// apply context attributes
			if(objectEnricher != null) {
				objectEnricher.accept(newFunction);	
			}

			Function oldFunction = null;
			if(previousFunctions!=null) {
				// search for an existing function with the same attributes
				try {
					oldFunction = previousFunctions.stream().filter(f->{
						return f.getAttributes().get(AbstractOrganizableObject.NAME).equals(newFunction.getAttributes().get(AbstractOrganizableObject.NAME));}).findFirst().get();							
				} catch (NoSuchElementException e) {}
			}

			if(oldFunction!=null) {
				// replace old function by the new one and ensure it keeps the same ID!
				// this is needed as long Plans refer to Functions by ID
				newFunction.setId(oldFunction.getId());
			}

			newFunction.setExecuteLocally(newFunctionPackage.isExecuteLocally());
			newFunction.setTokenSelectionCriteria(newFunctionPackage.getTokenSelectionCriteria());
			newFunction.setManaged(true);
			newFunction.addCustomField("functionPackageId",newFunctionPackage.getId().toString());
			
			// resolve the attribute values if necessary
			newFunction.setAttributes(newFunction.getAttributes().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                  e -> resolveAttribute(e.getKey(), e.getValue()))));
			
			functionRepository.saveFunction(newFunction);
			newFunctionIds.add(newFunction.getId());
		}
		
		// keep track of the created functions
		newFunctionPackage.setFunctions(newFunctionIds);
		
		//Make sure to preserve the new attributes over the context ones
		//for example: the target project of a migration is the one explicitly set in the send object
		//not the one from the context
		Map<String, String> preserved = new HashMap<>();
		preserved.putAll(newFunctionPackage.getAttributes());
		Map<String, String> newPackageAttributes = newFunctionPackage.getAttributes();
		if(objectEnricher != null) {
			newPackageAttributes.putAll(objectEnricher.getAdditionalAttributes());
		}
		newPackageAttributes.putAll(preserved);
		
		String resourceId = getResourceId(newFunctionPackage);
		if(resourceId != null) {
			Resource mainResource = resourceManager.getResource(resourceId);
			if(mainResource != null) {
				newPackageAttributes.put("name", mainResource.getResourceName());
			}
		} else {
			Path p = Paths.get(newFunctionPackage.getPackageLocation());
			newPackageAttributes.put("name", p.getFileName().toString());
		}

		// Apply new package attributes to corresponding resources
		applyAttributesToResource(resourceId, newFunctionPackage.getAttributes());
		applyAttributesToResource(getLibraryResourceId(newFunctionPackage), newFunctionPackage.getAttributes());
		
		registerWatcher(newFunctionPackage);
		
		newFunctionPackage = functionPackageAccessor.save(newFunctionPackage);

		return newFunctionPackage;
	}

	private Map<String, java.util.function.Function<String, String>> attributeResolvers = new ConcurrentHashMap<>();

	public void registerAttributeResolver(String key, java.util.function.Function<String, String> value) {
		attributeResolvers.put(key, value);
	}

	private String resolveAttribute(String key, String value) {
		// get the attribute resolver for the specified attribute 
		java.util.function.Function<String, String> attributeResolver = attributeResolvers.get(key);
		if(attributeResolver != null) {
			// Is it a value to be resolved i.e starting with @?
			if(value != null && value.startsWith("@")) {
				String valueToBeResolved = value.replaceFirst("@", "");
				String resolvedValue = attributeResolver.apply(valueToBeResolved);
				return resolvedValue;
			} else {
				return value;
			}
		} else {
			return value;
		}
	}
	
	private Map<String, String> excludeFieldFromMap(Map<String, String> map, Object key) {
		return map.entrySet().stream()
		.filter(x -> !key.equals(x.getKey()))
		.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	
	private void applyAttributesToResource(String resourceId, Map<String, String> packageAttributes) throws IOException {
		if(resourceId != null) {
			Resource resource = resourceManager.getResource(resourceId);
			if(resource.getAttributes() == null) {
				resource.setAttributes(new HashMap<>());
			}
			for (String key: packageAttributes.keySet()) {
				if (!key.equals(AbstractOrganizableObject.NAME)) {
					resource.getAttributes().put(key,packageAttributes.get(key));
				}
			}
			resourceManager.saveResource(resource);
		}
	}
	
	private void registerWatcher(FunctionPackage functionPackage) {
		if (changeWatcher!=null && !functionPackage.packageLocation.startsWith(FileResolver.RESOURCE_PREFIX)) {
			functionPackage.setWatchForChange(true);
			changeWatcher.registerWatcherForPackage(functionPackage);
		}
	}
	
	private void unregisterWatcher(FunctionPackage functionPackage) {
		if (changeWatcher!=null && !functionPackage.packageLocation.startsWith(FileResolver.RESOURCE_PREFIX)) {
			changeWatcher.unregisterWatcher(functionPackage);
		}
	}

	protected String getLibraryResourceId(FunctionPackage fpackage) {
		return getResourceSplitValue(fpackage.getPackageLibrariesLocation());
	}

	protected String getResourceId(FunctionPackage fpackage) {
		return getResourceSplitValue(fpackage.getPackageLocation());
	}

	protected String getResourceSplitValue(String resourceLocation) {
		if(resourceLocation != null && resourceLocation.startsWith(FileResolver.RESOURCE_PREFIX)) {
			String[] resourceIdSplit = resourceLocation.split(":");
			if(resourceIdSplit != null && resourceIdSplit.length > 1) {
				return resourceIdSplit[1];
			}
		}
		return null;
	}

	/**
	 * Get the list of {@link Function} contained in the provided package
	 * 
	 * @param functionPackage the {@link FunctionPackage} containing functions
	 * @return the list of {@link Function} found in the {@link FunctionPackage} 
	 * @throws Exception if any error occurs during loading
	 */
	public List<Function> getPackagePreview(FunctionPackage functionPackage) throws Exception {
		// Build the Functions with the corresponding handler
		FunctionPackageHandler handler = getPackageHandler(functionPackage);
		List<Function> functions = handler.buildFunctions(functionPackage, true);

		return functions;
	}

	/**
	 * Load Keyword package based on the first match on the resource name
	 * 
	 * @param resourceName the resource name to search with
	 * @return the first matching Keyword package
	 * @throws Exception if any error occurs during loading
	 */
	public FunctionPackage getPackageByResourceName(String resourceName) throws Exception {
		Resource lookedup = resourceManager.lookupResourceByName(resourceName);
		if(lookedup == null) {
			throw new Exception("Could not find resource with name: " + resourceName);
		}
		Map<String, String> criteria = new HashMap<>();
		criteria.put("resourceId", lookedup.getId().toString());
		return functionPackageAccessor.findByAttributes(criteria, "customFields");
	}

	private List<Function> getPackageFunctions(FunctionPackage functionPackage) {
		List<Function> currentFunctions = new ArrayList<>();
		functionPackage.functions.forEach(id->{
			Function function = functionRepository.getFunctionById(id.toString());
			if(function!=null) {
				currentFunctions.add(function);				
			}
		});
		return currentFunctions;
	}

	public FunctionPackage getFunctionPackage(String id) {
		return get(new ObjectId(id));
	}

	private FunctionPackage get(ObjectId id) {
		return functionPackageAccessor.get(id);
	}

	public void removeFunctionPackage(String id) {
		remove(new ObjectId(id));
	}

	private void remove(ObjectId id) {
		FunctionPackage functionPackage = functionPackageAccessor.get(id);	
		deleteFunctions(functionPackage);
		
		unregisterWatcher(functionPackage);
		
		functionPackageAccessor.remove(id);
		deleteResource(functionPackage.getPackageLocation());
		deleteResource(functionPackage.getPackageLibrariesLocation());
	}

	protected void deleteResource(String path) {
		String resolveResourceId = fileResolver.resolveResourceId(path);
		// Is it a resource?
		if(resolveResourceId != null) {
			// if yes, delete it
			try {
				resourceManager.deleteResource(resolveResourceId);
			}catch(RuntimeException e) {
				logger.warn("Dirty cleanup of FunctionPackage: an error occured while deleting one of the associated resources.", e);
			}
		}
	}

	public List<Function> getPackageFunctions(String functionPackageId) {
		FunctionPackage functionPackage = get(new ObjectId(functionPackageId));
		return functionPackage.getFunctions().stream().map(id->functionRepository.getFunctionById(id.toString())).collect(Collectors.toList());
	}

	@Override
	public void close() throws IOException {
		if(changeWatcher != null) {
			changeWatcher.close();
		}
	}
}
