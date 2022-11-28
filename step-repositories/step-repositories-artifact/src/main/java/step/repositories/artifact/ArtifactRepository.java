package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.core.repositories.*;
import step.functions.Function;
import step.repositories.artifact.ArtifactRepositoryClient.HTTPRepositoryProfile;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static step.planbuilder.BaseArtefacts.callPlan;

public class ArtifactRepository extends AbstractRepository {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepository.class);

    public static final String PARAM_URL_PACKAGE = "UrlPackage";
    public static final String PARAM_URL_DEPENDENCY = "UrlDependencies";
    public static final String PARAM_URL_AUTH = "UrlAuth";

    public static final String PARAM_THREAD_NUMBER = "NbThreads";

    // Store passwords as httprepository.passwords.user = password
    private static final String CONFIG_PASSWORDS = "httprepository.passwords.";

    private final Configuration configuration;
    private final ArtifactRepositoryClient artifactRepositoryClient;
    private final PlanAccessor planAccessor;
    private final ResourceManager resourceManager;

    private static class CacheEntry {
        Resource packageResource;
        String etagPackage;
        String packageUrl;

        Resource dependenciesResource;
        String etagDependencies;
        String dependenciesUrl;

        List<Plan> plans;
    }
    private final RepositoryCache<CacheEntry> cache;

    public ArtifactRepository(Configuration configuration, PlanAccessor planAccessor, ResourceManager resourceManager) {
        super();

        this.configuration = configuration;
        this.planAccessor = planAccessor;
        this.resourceManager = resourceManager;

        artifactRepositoryClient = new ArtifactRepositoryClient();

        cache = new RepositoryCache<>(
                this::downloadResources,
                this::updateResources,
                entry -> {
                    logger.info("Deleting package resource from " + entry.packageUrl + "...");
                    resourceManager.deleteResource(entry.packageResource.getId().toString());
                    if (entry.dependenciesResource != null) {
                        logger.info("Deleting dependencies resource from " + entry.dependenciesUrl + "...");
                        resourceManager.deleteResource(entry.dependenciesResource.getId().toString());
                    }
                },
                CacheBuilder.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(1, TimeUnit.HOURS));
    }

    @Override
    public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
        ArtefactInfo info = new ArtefactInfo();
        info.setName(ArtifactRepositoryClient.getFileName(getMandatoryPackageParameter(repositoryParameters)));
        info.setType(TestSet.class.getSimpleName());
        return info;
    }

    @Override
    public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) {
        TestSetStatusOverview overview = new TestSetStatusOverview();
        try {
            List<Plan> plans = cache.lock(repositoryParameters).getValue().plans;

            List<TestRunStatus> runs = new ArrayList<>();
            plans.forEach(plan -> {
                TestRunStatus run = new TestRunStatus(getPlanName(plan), ReportNodeStatus.NORUN);
                runs.add(run);
            });
            overview.setRuns(runs);
        } finally {
            cache.release(repositoryParameters);
        }
        return overview;
    }

    @Override
    public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) {
        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();
        try {
            List<Plan> plans = cache.lock(repositoryParameters).getValue().plans;

            PlanBuilder planBuilder = PlanBuilder.create();
            TestSet testSet = new TestSet(
                    Integer.parseInt(repositoryParameters.getOrDefault(PARAM_THREAD_NUMBER, "0")));
            testSet.getAttributes().put(AbstractArtefact.NAME,
                    ArtifactRepositoryClient.getFileName(getMandatoryPackageParameter(repositoryParameters)));

            planBuilder.startBlock(testSet);

            List<Function> functions = new ArrayList<>();
            plans.forEach(plan -> {
                String name = getPlanName(plan);
                plan.setVisible(false);
                functions.addAll(plan.getFunctions());
                planAccessor.save(plan);
                planBuilder.add(callPlan(plan.getId().toString(), name));
            });

            planBuilder.endBlock();

            Plan plan = planBuilder.build();
            plan.setVisible(false);
            plan.setFunctions(functions);
            enrichPlan(context, plan);
            planAccessor.save(plan);
            result.setPlanId(plan.getId().toString());

        } catch (Exception e) {
            logger.error("Error while importing artefact for " + context.getExecutionId(), e);
            errors.add("General error when trying to create the test set. Exception: " + e.getMessage());
        }

        result.setSuccessful(errors.isEmpty());
        result.setErrors(errors);

        return result;
    }

    @Override
    public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {
        cache.release(repositoryParameters);
    }

    private CacheEntry downloadResources(Map<String, String> repositoryParameters) {
        CacheEntry entry = new CacheEntry();

        HTTPRepositoryProfile profile = getHTTPRepositoryProfile(repositoryParameters);

        entry.packageUrl = getMandatoryPackageParameter(repositoryParameters);
        entry.packageResource = createResource(profile,entry.packageUrl);
        entry.etagPackage = artifactRepositoryClient.getETag(profile, entry.packageUrl);

        File resourceFile = resourceManager.getResourceFile(entry.packageResource.getId().toString()).getResourceFile();
        logger.info("Parsing package " + resourceFile + "...");
        entry.plans = new StepJarParser().getPlansForJar(resourceFile);
        logger.info("Parsed package " + resourceFile + ". Found " + entry.plans.size() + " plans.");

        entry.dependenciesUrl = repositoryParameters.get(PARAM_URL_DEPENDENCY);
        if (entry.dependenciesUrl != null) {
            entry.dependenciesResource = createResource(profile,entry.dependenciesUrl);
            entry.etagDependencies = artifactRepositoryClient.getETag(profile, entry.dependenciesUrl);
        }
        return entry;
    }

    private void updateResources(Map<String, String> repositoryParameters, CacheEntry entry) {
        String urlPackage = getMandatoryPackageParameter(repositoryParameters);
        HTTPRepositoryProfile profile = getHTTPRepositoryProfile(repositoryParameters);

        // should we update the package resource:
        boolean updated = false;
        String etag = artifactRepositoryClient.getETag(profile, urlPackage);
        if (!resourceManager.resourceExists(entry.packageResource.getId().toString())) {
            entry.packageResource = createResource(profile, entry.packageUrl);
            updated = true;
        } else if (!etag.equals(entry.etagPackage)) {
            entry.packageResource = updateResource(profile, entry.packageResource, entry.packageUrl);
            updated = true;
        }
        if (updated) {
            entry.etagPackage = etag;
            File resourceFile = resourceManager.getResourceFile(entry.packageResource.getId().toString()).getResourceFile();
            logger.info("Re-parsing package " + resourceFile + "...");
            entry.plans = new StepJarParser().getPlansForJar(resourceFile);
            logger.info("Updated package " + resourceFile + ". Found " + entry.plans.size() + " plans.");
        }
        String depUrl = repositoryParameters.get(PARAM_URL_DEPENDENCY);
        if (depUrl!=null && !depUrl.equals(entry.dependenciesUrl)) {
            entry.dependenciesUrl = depUrl;
        }
        if (entry.dependenciesUrl != null) {
            // should we update the dependency resource:
            etag = artifactRepositoryClient.getETag(profile, entry.dependenciesUrl);
            if (!resourceManager.resourceExists(entry.dependenciesResource.getId().toString())) {
                entry.dependenciesResource = createResource(profile, entry.dependenciesUrl);
            } else if (!etag.equals(entry.etagDependencies)) {
                entry.dependenciesResource = updateResource(profile, entry.dependenciesResource, entry.dependenciesUrl);
            }
            entry.etagDependencies = etag;
        }
    }

    private Resource createResource(HTTPRepositoryProfile profile, String url) {

        String name = ArtifactRepositoryClient.getFileName(url);
        logger.info("Downloading package from " + url + "...");
        return artifactRepositoryClient.downloadResource(profile, url, stream -> {
            try {
                Resource result = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, stream, name, false, null);
                logger.info("Got resource from " + url + " and saved it under resource " + result.getResourceName());
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Exception when trying to create resource '"+name+"'", e);
            }
        });
    }

    private Resource updateResource(HTTPRepositoryProfile profile, Resource resource, String url) {
        assert resource!=null;

        String name = resource.getResourceName();
        String id = resource.getId().toString();
        return artifactRepositoryClient.downloadResource(profile, url, stream -> {
            try {
                return resourceManager.saveResourceContent(id, stream, name);
            } catch (Exception e) {
                throw new RuntimeException("Exception when trying to update resource '"+name+"'", e);
            }
        });
    }

    private HTTPRepositoryProfile getHTTPRepositoryProfile(Map<String, String> repositoryParameters) {
        String user = repositoryParameters.get(PARAM_URL_AUTH);
        String password = null;
        if (user != null) {
            password = configuration.getProperty(CONFIG_PASSWORDS + user);
            if (password == null) {
                throw new RuntimeException("No password found for user '" + user + "' in the configuration file");
            }
        }
        return new HTTPRepositoryProfile(user, password);
    }

    private String getPlanName(Plan plan) {
        return plan.getAttributes().get(AbstractOrganizableObject.NAME);
    }

    private String getMandatoryPackageParameter(Map<String, String> repositoryParameters) {
        String result;
        if ((result = repositoryParameters.get(PARAM_URL_PACKAGE)) == null) {
            throw new RuntimeException("Parameter \"" + PARAM_URL_PACKAGE + "\" is missing");
        }
        return result;
    }
}
