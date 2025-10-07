package step.repositories.artifact;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.*;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.*;

public class MavenArtifactClient {

    private static final String AETHER_SNAPSHOT_PROPERTY = "aether.artifactResolver.snapshotNormalization";
    private final Settings settings;
    private final RepositorySystem repositorySystem;
    private final File localRepository;

    private static final Logger logger = LoggerFactory.getLogger(MavenArtifactClient.class);

    public MavenArtifactClient(String settingsXml, File localRepository) throws SettingsBuildingException {
        // SNAPSHOT jar files cannot be read without this property
        // let the user a chance to still override it
        if (System.getProperty(AETHER_SNAPSHOT_PROPERTY)==null) {
            System.setProperty(AETHER_SNAPSHOT_PROPERTY, "false");
        }

        settings = createSettings(settingsXml);
        repositorySystem = getRepositorySystem();
        this.localRepository = localRepository;
    }

    private static RepositorySystem getRepositorySystem() {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setServices(ModelBuilder.class, new DefaultModelBuilderFactory().newInstance());

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private static Settings createSettings(String settingsXml) throws SettingsBuildingException {
        DefaultSettingsBuilderFactory settingsBuilderFactory = new DefaultSettingsBuilderFactory();
        SettingsBuilder settingsBuilder = settingsBuilderFactory.newInstance();
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsSource(new StringSettingsSource(settingsXml));
        SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(request);
        Settings effectiveSettings = settingsBuildingResult.getEffectiveSettings();
        return effectiveSettings;
    }

    private static org.eclipse.aether.repository.Proxy convertFromMavenProxy(org.apache.maven.settings.Proxy proxy) {
        org.eclipse.aether.repository.Proxy result = null;
        if (proxy != null) {
            Authentication auth = new AuthenticationBuilder().addUsername(proxy.getUsername())
                    .addPassword(proxy.getPassword()).build();
            result = new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
        }
        return result;
    }

    public ResolvedMavenArtifact getArtifact(final Artifact artifact) throws ArtifactResolutionException {
        return getArtifact(artifact, null);
    }

    public ResolvedMavenArtifact getArtifact(final Artifact artifact, Long currentSnapshotTimestamp) throws ArtifactResolutionException {
        RepositorySystemSession session = getSession();
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        List<RemoteRepository> enabledRepositoriesFromProfile = getEnabledRepositoriesFromProfile();
        artifactRequest.setRepositories(enabledRepositoriesFromProfile);
        File result;

        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
        Artifact resolvedArtifact = artifactResult.getArtifact();
        if (resolvedArtifact != null) {
            result = resolvedArtifact.getFile();
            // 3. Get the latest metadata after download (same timestamp for all classifiers)
            SnapshotMetadata latestMetadata = fetchSnapshotMetadata(artifact, session, currentSnapshotTimestamp);
            return new ResolvedMavenArtifact(result, latestMetadata);
        } else {
            artifactResult.getExceptions().forEach(e -> logger.error("Error while resolving artifact " + artifact.toString(), e));
            throw new RuntimeException("The resolution of the artifact failed. See the logs for more details");
        }
    }

    private static boolean isNewSnapshot(Long existingSnapshotTimestamp, long snapshotRemoteTimestamp) {
        return existingSnapshotTimestamp == null || snapshotRemoteTimestamp <= 0 || snapshotRemoteTimestamp > existingSnapshotTimestamp;
    }

    public SnapshotMetadata fetchSnapshotMetadata(Artifact artifact, Long existingSnapshotTimestamp) {
        RepositorySystemSession session = getSession();
        return fetchSnapshotMetadata(artifact, session, existingSnapshotTimestamp);
    }

    private SnapshotMetadata fetchSnapshotMetadata(Artifact artifact, RepositorySystemSession session, Long existingSnapshotTimestamp) {
        List<RemoteRepository> repositories = getEnabledRepositoriesFromProfile();

        for (RemoteRepository repository : repositories) {
            try {
                // Use the correct DefaultMetadata constructor
                Metadata metadata = new DefaultMetadata(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        "maven-metadata.xml",
                        Metadata.Nature.RELEASE_OR_SNAPSHOT
                );

                MetadataRequest metadataRequest = new MetadataRequest();
                metadataRequest.setMetadata(metadata);
                metadataRequest.setRepository(repository); // Singular!

                List<MetadataResult> results = repositorySystem.resolveMetadata(
                        session, Collections.singletonList(metadataRequest));

                if (!results.isEmpty() && !results.get(0).isMissing()) {
                    File metadataFile = results.get(0).getMetadata().getFile();
                    if (metadataFile != null) {
                        return parseSnapshotMetadata(metadataFile, existingSnapshotTimestamp);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch metadata from repository {}: {}",
                        repository.getId(), e.getMessage());
            }
        }

        return null;
    }

    /**
     * Parses maven-metadata.xml file to extract snapshot information
     *
     * @param metadataFile The maven-metadata.xml file
     * @return SnapshotMetadata object with parsed information
     */
    public static SnapshotMetadata parseSnapshotMetadata(File metadataFile, Long existingSnapshotTimestamp) {
        if (metadataFile == null || !metadataFile.exists()) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(metadataFile);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Extract lastUpdated - keeping as reference but lastUpdated is usually inaccurate, it better to use the Snapshot timestamp
            String timestamp = getTextContent(xpath, doc, "//versioning/lastUpdated");

            // Extract build number
            String buildNumberStr = getTextContent(xpath, doc, "//versioning/snapshot/buildNumber");
            int buildNumber = -1;
            if (buildNumberStr != null && !buildNumberStr.isEmpty()) {
                try {
                    buildNumber = Integer.parseInt(buildNumberStr);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid build number in metadata: {}", buildNumberStr);
                }
            }



            long snapshotRemoteTimestamp = timestampToEpochMillis(timestamp);
            return new SnapshotMetadata(timestamp, snapshotRemoteTimestamp, buildNumber, isNewSnapshot(existingSnapshotTimestamp, snapshotRemoteTimestamp));

        } catch (Exception e) {
            logger.error("Failed to parse snapshot metadata from file: {}", metadataFile, e);
            return null;
        }
    }

    /**
     * Converts Maven timestamp format (yyyyMMdd.HHmmss) to epoch milliseconds
     */
    public static long timestampToEpochMillis(String mavenTimestamp) {
        if (mavenTimestamp == null || mavenTimestamp.trim().isEmpty() || mavenTimestamp.length() != 14) {
            return -1;
        }

        try {
            // Parse format: yyyyMMddHHmmss -> yyyy-MM-dd HH:mm:ss

            // Extract components
            int year = Integer.parseInt(mavenTimestamp.substring(0, 4));
            int month = Integer.parseInt(mavenTimestamp.substring(4, 6));
            int day = Integer.parseInt(mavenTimestamp.substring(6, 8));

            int hour = Integer.parseInt(mavenTimestamp.substring(8, 10));
            int minute = Integer.parseInt(mavenTimestamp.substring(10, 12));
            int second = Integer.parseInt(mavenTimestamp.substring(12, 14));

            // Create LocalDateTime and convert to epoch
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.of(year, month, day, hour, minute, second);
            return dateTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();

        } catch (Exception e) {
            // Return -1 if parsing fails
            return -1;
        }
    }


    /**
     * Helper method to extract text content from XML using XPath
     */
    private static String getTextContent(XPath xpath, Document doc, String expression) {
        try {
            Node node = (Node) xpath.evaluate(expression, doc, XPathConstants.NODE);
            return node != null ? node.getTextContent().trim() : null;
        } catch (Exception e) {
            logger.debug("XPath expression '{}' not found or invalid", expression);
            return null;
        }
    }

    private DefaultRepositorySystemSession getSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setOffline(false);
        // Force the update of SNAPSHOTS. This doesn't affect releases.
        session.setUpdatePolicy("always");

        Proxy activeProxy = settings.getActiveProxy();
        if (activeProxy != null) {
            DefaultProxySelector dps = new DefaultProxySelector();
            dps.add(convertFromMavenProxy(activeProxy), activeProxy.getNonProxyHosts());
            session.setProxySelector(dps);
        }

        final DefaultMirrorSelector mirrorSelector = createMirrorSelector(settings);
        final LazyAuthenticationSelector authSelector = createAuthSelector(settings, mirrorSelector);

        session.setMirrorSelector(mirrorSelector);
        session.setAuthenticationSelector(authSelector);

        LocalRepository localRepo = new LocalRepository(localRepository);
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    private List<RemoteRepository> getEnabledRepositoriesFromProfile() {
        Set<RemoteRepository> settingsRepos = new HashSet<>();
        List<String> activeProfiles = settings.getActiveProfiles();

        // "Active by default" profiles must be added separately, since they are not recognized as active ones
        for (Profile profile : settings.getProfiles()) {
            Activation activation = profile.getActivation();
            if (activation != null && activation.isActiveByDefault()) {
                activeProfiles.add(profile.getId());
            }
        }

        Map<String, Profile> profiles = settings.getProfilesAsMap();

        // Collect all repositories declared in all active profiles
        for (String id : activeProfiles) {
            Profile profile = profiles.get(id);
            if (profile != null) {
                List<org.apache.maven.settings.Repository> repositories = profile.getRepositories();
                for (org.apache.maven.settings.Repository repository : repositories) {
                    settingsRepos.add(new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository
                            .getUrl()).build());
                }
            }
        }

        final DefaultMirrorSelector mirrorSelector = createMirrorSelector(settings);

        final List<RemoteRepository> mirrorsForSettingsRepos = new ArrayList<>();
        for (Iterator<RemoteRepository> iter = settingsRepos.iterator(); iter.hasNext(); ) {
            RemoteRepository settingsRepository = iter.next();
            RemoteRepository repoMirror = mirrorSelector.getMirror(settingsRepository);
            // If a mirror is available for a repository, then remove the repo, and use the mirror instead
            if (repoMirror != null) {
                iter.remove();
                mirrorsForSettingsRepos.add(repoMirror);
            }
        }
        // We now have a collection of mirrors and un-mirrored repositories.
        settingsRepos.addAll(mirrorsForSettingsRepos);

        Set<RemoteRepository> enrichedRepos = new HashSet<>();
        LazyAuthenticationSelector authSelector = createAuthSelector(settings, mirrorSelector);
        for (RemoteRepository settingsRepo : settingsRepos) {
            // Obtain the Authentication for the repository or it's mirror
            Authentication auth = authSelector.getAuthentication(settingsRepo);
            // All RemoteRepositories (Mirrors and Repositories) constructed so far lack Authentication info.
            // Use the settings repo as the prototype and create an enriched repo with the Authentication.
            enrichedRepos.add(new RemoteRepository.Builder(settingsRepo).setAuthentication(auth).build());
        }
        return new ArrayList<>(enrichedRepos);
    }

    private LazyAuthenticationSelector createAuthSelector(final Settings settings,
                                                          final DefaultMirrorSelector mirrorSelector) {
        LazyAuthenticationSelector authSelector = new LazyAuthenticationSelector(mirrorSelector);
        for (Server server : settings.getServers()) {
            authSelector.add(
                    server.getId(),
                    new AuthenticationBuilder().addUsername(server.getUsername()).addPassword(server.getPassword())
                            .addPrivateKey(server.getPrivateKey(), server.getPassphrase()).build());
        }
        return authSelector;
    }

    private DefaultMirrorSelector createMirrorSelector(Settings settings) {
        final DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        final List<Mirror> mirrors = settings.getMirrors();
        if (mirrors != null) {
            for (Mirror mirror : mirrors) {
                mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                        mirror.getMirrorOfLayouts());
            }
        }
        return mirrorSelector;
    }
}
