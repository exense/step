package step.functions.packages;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.Function;
import step.functions.manager.FunctionManager;
import step.functions.type.FunctionTypeException;
import step.resources.Resource;
import step.resources.ResourceManager;

/**
 * This class is responsible for the handling of {@link FunctionPackage}
 * 
 * It is responsible for the import of {@link FunctionPackage}. The import of a
 * {@link FunctionPackage} means:
 * <ul>
 * <li>the persistence of the {@link FunctionPackage} itself</li>
 * <li>the definition of the {@link Function} contained in the package</li>
 * <li>the registration of change watcher of the package file of the
 * {@link FunctionPackage}</li>
 * </ul>
 *
 */
public class FunctionPackageManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackageManager.class);

	private static final String WATCH_FOR_CHANGE = "plugins.FunctionPackagePlugin.watchForChange";
	private static final String WATCHER_INTERVAL = "plugins.FunctionPackagePlugin.watchForChange.interval";

	private final FunctionPackageAccessor functionPackageAccessor;
	private final FunctionManager functionRepository;
	private final ResourceManager resourceManager;
	private final FileResolver fileResolver;
	private FunctionPackageChangeWatcher changeWatcher;
	private final ObjectHookRegistry objectHookRegistry;

	private final List<FunctionPackageHandler> packageHandlers = new ArrayList<>();
	private final Map<String, java.util.function.Function<String, String>> attributeResolvers = new ConcurrentHashMap<>();

	public FunctionPackageManager(FunctionPackageAccessor functionPackageAccessor, FunctionManager functionRepository,
			ResourceManager resourceManager, FileResolver fileResolver, Configuration configuration,
			ObjectHookRegistry objectHookRegistry) {
		super();
		this.functionPackageAccessor = functionPackageAccessor;
		this.functionRepository = functionRepository;
		this.resourceManager = resourceManager;
		this.fileResolver = fileResolver;
		this.objectHookRegistry = objectHookRegistry;

		if (configuration.getPropertyAsBoolean(WATCH_FOR_CHANGE, true)) {
			int interval = configuration.getPropertyAsInteger(WATCHER_INTERVAL, 60000);
			changeWatcher = new FunctionPackageChangeWatcher(functionPackageAccessor, this, interval);
		}
	}

	public void start() {
		if (changeWatcher != null) {
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

	public void registerAttributeResolver(String key, java.util.function.Function<String, String> value) {
		attributeResolvers.put(key, value);
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
		List<Function> functions = handler.buildFunctions(functionPackage, true, null);
		return functions;
	}

	/**
	 * Adds or updates a {@link FunctionPackage} This triggers the import or
	 * re-import of the {@link Function}s contained in the package
	 * 
	 * @param newFunctionPackage the {@link FunctionPackage} to be loaded
	 * @return the updated {@link FunctionPackage}
	 * @throws Exception if any error occurs during loading
	 */
	public FunctionPackage addOrUpdateFunctionPackage(FunctionPackage newFunctionPackage)
			throws Exception {
		FunctionPackage previousFunctionPackage = null;
		if (newFunctionPackage.getId() != null) {
			previousFunctionPackage = get(newFunctionPackage.getId());
			cleanupObsoleteResource(previousFunctionPackage, newFunctionPackage);
		}
		return addOrUpdateFunctionPackage(previousFunctionPackage, newFunctionPackage);
	}

	/**
	 * Reloads a {@link FunctionPackage}. This triggers a re-import of the
	 * {@link Function}s contained in the package
	 * 
	 * @param functionPackageId the ID of the {@link FunctionPackage} to be reloaded
	 * @return the updated {@link FunctionPackage}
	 * @throws Exception if any error occurs during reloading
	 */
	public FunctionPackage reloadFunctionPackage(String functionPackageId)
			throws Exception {
		assert functionPackageId != null;
		FunctionPackage functionPackage = getFunctionPackage(functionPackageId);
		assert functionPackage != null;
		return addOrUpdateFunctionPackage(functionPackage, functionPackage);
	}

	public FunctionPackage getFunctionPackage(String id) {
		return get(new ObjectId(id));
	}

	public List<Function> getPackageFunctions(String functionPackageId) {
		return getPackageFunctions(getFunctionPackage(functionPackageId));
	}

	public void removeFunctionPackage(String id) {
		remove(new ObjectId(id));
	}

	@Override
	public void close() throws IOException {
		if (changeWatcher != null) {
			changeWatcher.close();
		}
	}

	private FunctionPackageHandler getPackageHandler(FunctionPackage functionPackage)
			throws UnsupportedFunctionPackageType {
		return packageHandlers.stream().filter(f -> f.isValidForPackage(functionPackage)).findFirst()
				.orElseThrow(() -> new UnsupportedFunctionPackageType(
						"Unsupported package type: " + functionPackage.getPackageLocation()));
	}

	private void cleanupObsoleteResource(FunctionPackage previousFunctionPackage, FunctionPackage newFunctionPackage) {
		if (previousFunctionPackage != null && newFunctionPackage != null) {
			// cleanup main resource
			if (previousFunctionPackage.getPackageLocation() != null
					&& newFunctionPackage.getPackageLocation() != null) {
				String previousResourceId = getResourceId(previousFunctionPackage);
				String newResourceId = getResourceId(newFunctionPackage);
				if (previousResourceId != null && !previousResourceId.equals(newResourceId)) {
					if (resourceManager.resourceExists(previousResourceId)) {
						resourceManager.deleteResource(previousResourceId);
					}
				}
			}

			// cleanup library resource
			if (previousFunctionPackage.getPackageLibrariesLocation() != null
					&& newFunctionPackage.getPackageLibrariesLocation() != null) {
				String previousResourceId = getLibraryResourceId(previousFunctionPackage);
				String newResourceId = getLibraryResourceId(newFunctionPackage);
				if (previousResourceId != null && !previousResourceId.equals(newResourceId)) {
					if (resourceManager.resourceExists(previousResourceId)) {
						resourceManager.deleteResource(previousResourceId);
					}
				}
			}
		}
	}

	private List<Function> deleteFunctions(FunctionPackage previousFunctionPackage) {
		List<Function> previousFunctions = getPackageFunctions(previousFunctionPackage);
		previousFunctions.forEach(function -> {
			try {
				functionRepository.deleteFunction(function.getId().toString());
			} catch (FunctionTypeException e) {
				logger.error("Error while deleting function " + function.getId().toString(), e);
			}
		});
		return previousFunctions;
	}

	private FunctionPackage addOrUpdateFunctionPackage(FunctionPackage previousFunctionPackage,
			FunctionPackage newFunctionPackage) throws Exception {
		String packageLocation = newFunctionPackage.getPackageLocation();
		if (packageLocation == null || packageLocation.trim().length() == 0) {
			throw new Exception("Empty package file");
		}

		// Auto detect the appropriate package handler
		FunctionPackageHandler handler = getPackageHandler(newFunctionPackage);
		
		// resolve the attribute values if necessary
		if (newFunctionPackage.getAttributes() != null) {
			newFunctionPackage.setAttributes(newFunctionPackage.getAttributes().entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> resolveAttribute(e.getKey(), e.getValue()))));
		}

		// apply context attributes of the function package to the function
		AbstractContext context = new AbstractContext() {};
		objectHookRegistry.rebuildContext(context, newFunctionPackage);
		ObjectEnricher objectEnricher = objectHookRegistry.getObjectEnricher(context);

		// Remove functions from previous package
		List<Function> previousFunctions = null;
		if (previousFunctionPackage != null) {
			previousFunctions = deleteFunctions(previousFunctionPackage);
			unregisterWatcher(previousFunctionPackage);
		}

		// Build the Functions with the appropriate handler
		List<Function> functions = handler.buildFunctions(newFunctionPackage, false, objectEnricher);

		List<ObjectId> newFunctionIds = new ArrayList<>();
		for (Function newFunction : functions) {
			// apply packageAttributes
			if (newFunctionPackage.getPackageAttributes() != null) {
				newFunction.getAttributes().putAll(newFunctionPackage.getPackageAttributes());
			}

			objectEnricher.accept(newFunction);

			// search for an existing function with the same name and reuse its ID
			// this is needed as long Plans refer to Functions by ID
			if (previousFunctions != null) {
				previousFunctions.stream().filter(f -> f.getName().equals(newFunction.getName()))
						.findFirst().ifPresent(oldFunction -> newFunction.setId(oldFunction.getId()));
			}

			newFunction.setManaged(true);
			newFunction.setExecuteLocally(newFunctionPackage.isExecuteLocally());
			newFunction.setTokenSelectionCriteria(newFunctionPackage.getTokenSelectionCriteria());
			newFunction.addCustomField(FunctionPackageEntity.FUNCTION_PACKAGE_ID,
					newFunctionPackage.getId().toString());

			functionRepository.saveFunction(newFunction);
			newFunctionIds.add(newFunction.getId());
		}

		// keep track of the created functions
		newFunctionPackage.setFunctions(newFunctionIds);

		// set the name of the function package
		String name = buildFunctionPackageName(newFunctionPackage);
		newFunctionPackage.setName(name);

		registerWatcher(newFunctionPackage);

		// persist the changes
		newFunctionPackage = functionPackageAccessor.save(newFunctionPackage);

		return newFunctionPackage;
	}

	private String buildFunctionPackageName(FunctionPackage newFunctionPackage) {
		String name;
		String resourceId = getResourceId(newFunctionPackage);
		if (resourceId != null) {
			Resource mainResource = resourceManager.getResource(resourceId);
			if (mainResource != null) {
				name = mainResource.getResourceName();
			} else {
				throw new RuntimeException("The resource with id " + resourceId + " could not be found");
			}
		} else {
			Path p = Paths.get(newFunctionPackage.getPackageLocation());
			name = p.getFileName().toString();
		}
		return name;
	}

	private String resolveAttribute(String key, String value) {
		// get the attribute resolver for the specified attribute
		java.util.function.Function<String, String> attributeResolver = attributeResolvers.get(key);
		if (attributeResolver != null) {
			// Is it a value to be resolved i.e starting with @?
			if (value != null && value.startsWith("@")) {
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

	private void registerWatcher(FunctionPackage functionPackage) {
		if (changeWatcher != null && !fileResolver.isResource(functionPackage.getPackageLocation())) {
			functionPackage.setWatchForChange(true);
			changeWatcher.registerWatcherForPackage(functionPackage);
		}
	}

	private void unregisterWatcher(FunctionPackage functionPackage) {
		if (changeWatcher != null && !fileResolver.isResource(functionPackage.getPackageLocation())) {
			changeWatcher.unregisterWatcher(functionPackage);
		}
	}

	private String getLibraryResourceId(FunctionPackage fpackage) {
		return resolveResourceId(fpackage.getPackageLibrariesLocation());
	}

	private String getResourceId(FunctionPackage fpackage) {
		return resolveResourceId(fpackage.getPackageLocation());
	}

	private String resolveResourceId(String resourceLocation) {
		return fileResolver.resolveResourceId(resourceLocation);
	}

	private List<Function> getPackageFunctions(FunctionPackage functionPackage) {
		return functionPackage.functions.stream().map(id -> functionRepository.getFunctionById(id.toString()))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private FunctionPackage get(ObjectId id) {
		return functionPackageAccessor.get(id);
	}

	private void remove(ObjectId id) {
		FunctionPackage functionPackage = functionPackageAccessor.get(id);
		deleteFunctions(functionPackage);

		unregisterWatcher(functionPackage);

		functionPackageAccessor.remove(id);
		deleteResource(functionPackage.getPackageLocation());
		deleteResource(functionPackage.getPackageLibrariesLocation());
	}

	private void deleteResource(String path) {
		String resolveResourceId = fileResolver.resolveResourceId(path);
		// Is it a resource?
		if (resolveResourceId != null) {
			// if yes, delete it
			try {
				resourceManager.deleteResource(resolveResourceId);
			} catch (RuntimeException e) {
				logger.warn(
						"Dirty cleanup of FunctionPackage: an error occured while deleting one of the associated resources.",
						e);
			}
		}
	}
}
