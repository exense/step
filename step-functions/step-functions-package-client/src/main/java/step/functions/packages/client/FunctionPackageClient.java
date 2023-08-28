package step.functions.packages.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import step.functions.packages.FunctionPackage;

public interface FunctionPackageClient extends Closeable {

	/**
	 * Creates a new Keyword package
	 * 
	 * @param packageLibraryFile a Zip
	 * @param packageFile the Jar/DLL file containing the Keyword definitions. The file will be uploaded to the Controller
	 * @param packageAttributes the attributes of the Keyword (ex: version, app, etc)
	 * @param trackingField (optional) the attribute to determine the uniqueness of the function package
	 * @return the newly created {@link FunctionPackage}
	 * @throws IOException in case of any error
	 */
	FunctionPackage newKeywordPackage(File packageLibraryFile, File packageFile, Map<String, String> packageAttributes, String trackingField)
			throws IOException;


	/**
	 * Update an existing Keyword package identified by its ObjectId
	 * 
	 * @param previousPackage the older version of the {@link FunctionPackage} obtained at creation time (i.e returned by newKeywordPackage)
	 * @param packageLibraryFile a Zip
	 * @param packageFile the Jar/DLL file containing the Keyword definitions. The file will be uploaded to the Controller
	 * @param packageAttributes (optional) the attributes the keyword package can be identified with, by default the resource name will be used
	 * @param trackingField (optional) the attribute to determine the uniqueness of the function package
	 * @return the updated {@link FunctionPackage}
	 * @throws IOException in case of any error
	 */
	FunctionPackage updateKeywordPackageById(FunctionPackage previousPackage, File packageLibraryFile, File packageFile,
			Map<String, String> packageAttributes, String trackingField) throws IOException;


	/**
	 * Creates a new Keyword package
	 *
	 * @param packageLib the zip with additional libraries (the binary file to be loaded to the Controller or id of the already uploaded resource).
	 * @param packageFile the Jar/DLL file containing the Keyword definitions. The file will be uploaded to the Controller
	 * @param packageAttributes the attributes of the Keyword (ex: version, app, etc)
	 * @param trackingField (optional) the attribute to determine the uniqueness of the function package
	 * @return the newly created {@link FunctionPackage}
	 * @throws IOException in case of any error
	 */
	FunctionPackage newKeywordPackageWithLibReference(LibFileReference packageLib, File packageFile, Map<String, String> packageAttributes, String trackingField) throws IOException;

	/**
	 * Update an existing Keyword package identified by its ObjectId
	 *
	 * @param previousPackage the older version of the {@link FunctionPackage} obtained at creation time (i.e returned by newKeywordPackage)
	 * @param packageLib the zip with additional libraries (the binary file to be loaded to the Controller or id of the already uploaded resource).
	 * @param packageFile the Jar/DLL file containing the Keyword definitions. The file will be uploaded to the Controller
	 * @param packageAttributes (optional) the attributes the keyword package can be identified with, by default the resource name will be used
	 * @param trackingField (optional) the attribute to determine the uniqueness of the function package
	 * @return the updated {@link FunctionPackage}
	 * @throws IOException in case of any error
	 */
	FunctionPackage updateKeywordPackageWithLibReference(FunctionPackage previousPackage, LibFileReference packageLib, File packageFile, Map<String, String> packageAttributes, String trackingField) throws IOException;

	/**
	 * Delete an existing Keyword package
	 * 
	 * @param packageId the ID of the package
	 */
	void deleteKeywordPackage(String packageId);
	
	/**
	 * Update an existing Keyword package with a new version of the resource(s), the package is implicitly managed via resource name
	 * Warning: if multiple resources are created with the same name, or if multiple keyword packages reference the same resource,
	 * only the first match will be updated.
	 * 
	 * @deprecated This service has been removed. Lookup by resourceName isn't supported anymore. Use updateKeywordPackageById instead.
	 * @param packageLibraryFile a Zip
	 * @param packageFile the Jar/DLL file containing the Keyword definitions. The file will be uploaded to the Controller
	 * @param packageAttributes (optional) the attributes the keyword package can be identified with, by default the resource name will be used
	 * @return the updated {@link FunctionPackage}
	 * @throws IOException in case of any error
	 */
	FunctionPackage updateResourceBasedKeywordPackage(File packageLibraryFile, File packageFile,
			Map<String, String> packageAttributes) throws IOException;

	/**
	 * Retrieve a {@link FunctionPackage} object based on the resourceName associated with it.
	 * Warning: if multiple resources are created with the same name, or if multiple keyword packages reference the same resource,
	 * only the first match will be updated.
	 * 
	 * @deprecated This service has been removed. Lookup by resourceName isn't supported anymore. Use addOrUpdateKeywordPackage instead.
	 * @param resourceName the name of the Resource that the searched {@link FunctionPackage} relies on
	 * @return the corresponding {@link FunctionPackage}, if any match occurred
	 * @throws IOException in case of any error
	 */
	FunctionPackage lookupPackageByResourceName(String resourceName) throws IOException;

}