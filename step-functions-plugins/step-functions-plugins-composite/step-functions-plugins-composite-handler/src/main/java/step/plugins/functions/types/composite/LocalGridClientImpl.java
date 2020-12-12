package step.plugins.functions.types.composite;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.DefaultTokenLifecycleStrategy;
import step.grid.client.GridClientConfiguration;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.tokenpool.Interest;

public class LocalGridClientImpl extends AbstractGridClientImpl {

	private final ApplicationContextBuilder applicationContextBuilder;
	
	public LocalGridClientImpl(ApplicationContextBuilder applicationContextBuilder) {
		super(new GridClientConfiguration(), new DefaultTokenLifecycleStrategy(), null);
		this.applicationContextBuilder = applicationContextBuilder;
		initLocalAgentServices();
		initLocalMessageHandlerPool();
		
	}
	
	protected void initLocalAgentServices() {
		FileManagerClient fileManagerClient = new FileManagerClient() {
			@Override
			public FileVersion requestFileVersion(FileVersionId fileVersionId) throws FileManagerException {
				return getRegisteredFile(fileVersionId);
			}
	
			@Override
			public void removeFileVersionFromCache(FileVersionId fileVersionId) {
				unregisterFile(fileVersionId);
			}
		};
		
		localAgentTokenServices = new AgentTokenServices(fileManagerClient);
		localAgentTokenServices.setApplicationContextBuilder(applicationContextBuilder);
	}

	protected void initLocalMessageHandlerPool() {
		localMessageHandlerPool = new MessageHandlerPool(localAgentTokenServices);
	}

	@Override
	public FileVersion registerFile(File file) throws FileManagerException {
		return new FileVersion(file, new FileVersionId(file.getAbsolutePath(), ""), false);
	}

	@Override
	public FileVersion getRegisteredFile(FileVersionId fileVersionId) throws FileManagerException {
		return new FileVersion(new File(fileVersionId.getFileId()), fileVersionId, false);
	}

	@Override
	public void unregisterFile(FileVersionId fileVersionId) {
		
	}

	protected final ConcurrentHashMap<String, FileVersion> resourceCache = new ConcurrentHashMap<>();
	
	@Override
	public FileVersion registerFile(InputStream inputStream, String fileName, boolean isDirectory)
			throws FileManagerException {
		FileVersion fileVersion = resourceCache.computeIfAbsent(fileName, f->{
			try {
				File file = File.createTempFile(fileName + "-" + UUID.randomUUID(), fileName.substring(fileName.lastIndexOf(".")));
				Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				file.deleteOnExit();
				return registerFile(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return fileVersion;
	}

	private RuntimeException unsupportedOperation() {
		return new RuntimeException("Unsupported operation");
	}

	@Override
	public TokenWrapper getLocalTokenHandle() {
		return super.getLocalTokenHandle();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> selectionCriteria, boolean createSession)
			throws AgentCommunicationException {
		throw unsupportedOperation();
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> selectionCriteria, boolean createSession,
			TokenWrapperOwner tokenOwner) throws AgentCommunicationException {
		throw unsupportedOperation();
	}
}