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
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (System.getProperty(AETHER_SNAPSHOT_PROPERTY)!=null && System.getProperty(AETHER_SNAPSHOT_PROPERTY).equals("true")) {
            logger.info("System property '"+AETHER_SNAPSHOT_PROPERTY+"' will be override to 'false'");
        }
        System.setProperty(AETHER_SNAPSHOT_PROPERTY, "false");

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

    public File getArtifact(final Artifact artifact) throws ArtifactResolutionException {
        RepositorySystemSession session = getSession();
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(getEnabledRepositoriesFromProfile());
        File result;

        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
        Artifact resolvedArtifact = artifactResult.getArtifact();
        if (resolvedArtifact != null) {
            result = resolvedArtifact.getFile();
        } else {
            artifactResult.getExceptions().forEach(e -> logger.error("Error while resolving artifact " + artifact.toString(), e));
            throw new RuntimeException("The resolution of the artifact failed. See the logs for more details");
        }

        return result;
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
