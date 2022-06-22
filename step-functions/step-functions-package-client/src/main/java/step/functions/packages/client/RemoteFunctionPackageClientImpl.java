package step.functions.packages.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

import step.attachments.FileResolver;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.functions.packages.FunctionPackage;
import step.resources.Resource;
import step.resources.SimilarResourceExistingException;

public class RemoteFunctionPackageClientImpl extends AbstractRemoteClient implements FunctionPackageClient {

	private RemoteResourceManager remoteResourceManager;

	public RemoteFunctionPackageClientImpl() {
		super();
		remoteResourceManager = new RemoteResourceManager();
	}

	public RemoteFunctionPackageClientImpl(ControllerCredentials credentials) {
		super(credentials);
		remoteResourceManager = new RemoteResourceManager(credentials);
	}
	
	private FunctionPackage addOrUpdateKeywordPackage(FunctionPackage previousPackage, File packageLibraryFile, File packageFile, Map<String, String> packageAttributes) throws IOException {
		FunctionPackage functionPackage = null;
		if(previousPackage != null) {
			functionPackage = previousPackage;
		}else {
			functionPackage = new FunctionPackage();
		}
		Resource packageLibraryResource = null;
		if(packageLibraryFile != null) {
			packageLibraryResource = upload(packageLibraryFile);
			functionPackage.setPackageLibrariesLocation(FileResolver.RESOURCE_PREFIX+packageLibraryResource.getId().toString());
		}

		Resource packageResource = upload(packageFile);
		functionPackage.setPackageLocation(FileResolver.RESOURCE_PREFIX+packageResource.getId().toString());

		functionPackage.setPackageAttributes(packageAttributes);
		functionPackage.setWatchForChange(false);

		Builder b = requestBuilder("/rest/functionpackages/");
		Entity<?> entity = Entity.entity(functionPackage, MediaType.APPLICATION_JSON);
		
		return executeRequest(()->b.post(entity)).readEntity(FunctionPackage.class); 
	}

	@Override
	public FunctionPackage newKeywordPackage(File packageLibraryFile, File packageFile, Map<String, String> packageAttributes) throws IOException {
		return addOrUpdateKeywordPackage(null, packageLibraryFile, packageFile, packageAttributes);
	}
	
	@Override
	public FunctionPackage updateKeywordPackageById(FunctionPackage previousPackage, File packageLibraryFile, File packageFile, Map<String, String> packageAttributes) throws IOException {
		return addOrUpdateKeywordPackage(previousPackage, packageLibraryFile, packageFile, packageAttributes);
	}

	@Override
	public void deleteKeywordPackage(String packlageId) {
		Builder b = requestBuilder("/rest/functionpackages/"+packlageId);
		executeRequest(()->b.delete());
	}

	@Override
	public FunctionPackage updateResourceBasedKeywordPackage(File packageLibraryFile, File packageFile,
			Map<String, String> packageAttributes) throws IOException {
		throw new RuntimeException(
				"This service has been removed. Lookup by resourceName isn't supported anymore. Use updateKeywordPackageById instead.");
	}

	@Override
	public FunctionPackage lookupPackageByResourceName(String resourceName) throws IOException {
		throw new RuntimeException(
				"This service has been removed. Lookup by resourceName isn't supported anymore. Use addOrUpdateKeywordPackage instead.");
	}

	protected Resource upload(File file) throws IOException {
		try {
			return remoteResourceManager.createResource("functions", new FileInputStream(file), file.getName(), false, null);
		} catch (IOException e) {
			throw e;
		} catch (SimilarResourceExistingException e) {
			throw new RuntimeException("Unexpected similar resource error. This should never occur.", e);
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		remoteResourceManager.close();
	}

}
