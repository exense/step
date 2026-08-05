package step.ide.api;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.ide.IDEKeywordPropertiesPlugin;
import step.ide.LocalIDEState;
import step.ide.ai.AutomationPackageSpecStore;
import step.ide.ai.IDEAiConfiguration;
import step.ide.ai.SpecMarkdownParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI assisted test case creation in the IDE.
 * <p>
 * Deliberately a dedicated endpoint rather than the generic execution endpoint: the frontend must not need to know
 * where the AI agent package lives, the execution diversion must stay a pure "run the opened package" path, and
 * writing the spec files is a precondition of the launch.
 * <p>
 * Consistently with {@link LocalIDEServices}, these endpoints are not secured: the IDE is a local single user process.
 */
@Path("/local/ide/ai")
@Tag(name = "IDE")
public class LocalIDEAiServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDEAiServices.class);

    public static final String MODE_CREATE = "create";
    public static final String MODE_REGENERATE = "regenerate";

    public static final String PARAM_TARGET_DIRECTORY = "apDirectory";
    public static final String PARAM_INPUT = "instructions";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpecMarkdownParser specMarkdownParser = new SpecMarkdownParser();

    @PostConstruct
    public void init() throws Exception {
        super.init();
    }

    /** One test case as submitted by the structured form of the dialog. */
    public record AiTestCaseInput(String name, String spec, String mode, String hints) {
    }

    /**
     * Either {@code testCases} (structured form mode) or {@code specText} (free text mode) must be provided.
     */
    public record AiGenerateRequest(List<AiTestCaseInput> testCases, String specText, String hints) {
    }

    public record AiSpec(String testCaseName, String spec, boolean exists, String specFile) {
    }

    /**
     * Deliberately a JSON object rather than the bare execution id as text/plain: the generated Angular client always
     * sends {@code Accept: application/json}, so a text/plain endpoint is answered with a 406 unless every caller
     * bypasses the client with a raw HttpClient call.
     */
    public record AiGenerateResponse(String executionId) {
    }

    /**
     * @param apiKeyConfigured whether an Anthropic API key is configured. Reported as a boolean only, the value never
     *                         leaves the backend. Does not gate the feature: the agent may also pick the key up from
     *                         its own environment.
     */
    public record AiConfiguration(boolean available, boolean apiKeyConfigured, String message) {
    }

    /** The payload handed to the agent as the {@value #PARAM_INPUT} execution parameter. */
    private record AiInput(List<AiTestCaseInput> testCases, String hints) {
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public AiConfiguration getConfiguration() {
        Configuration ideConfiguration = LocalIDEState.get().getConfiguration();
        IDEAiConfiguration configuration = IDEAiConfiguration.from(ideConfiguration);
        String reason = configuration.unavailabilityReason();
        String apiKey = ideConfiguration == null ? null
            : ideConfiguration.getProperty(IDEKeywordPropertiesPlugin.KEYWORD_PROPERTY_PREFIX + IDEKeywordPropertiesPlugin.ENV_ANTHROPIC_API_KEY);
        return new AiConfiguration(reason == null, apiKey != null && !apiKey.isBlank(), reason);
    }

    /**
     * Returns the stored spec of a test case. Always answers 200: a missing spec is a normal situation (the plan may
     * have been generated elsewhere), reported through {@link AiSpec#exists()}.
     */
    @GET
    @Path("spec")
    @Produces(MediaType.APPLICATION_JSON)
    public AiSpec getSpec(@QueryParam("testCaseName") String testCaseName) {
        AutomationPackageSpecStore specStore = specStore();
        try {
            java.nio.file.Path specPath = specStore.specPath(testCaseName);
            return specStore.read(testCaseName)
                .map(spec -> new AiSpec(testCaseName, spec, true, specStore.relativize(specPath)))
                .orElseGet(() -> new AiSpec(testCaseName, null, false, specStore.relativize(specPath)));
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        } catch (IOException e) {
            logger.error("Unable to read the spec of test case {}", testCaseName, e);
            throw new ControllerServiceException(500, e.getMessage());
        }
    }

    /**
     * Writes the specs and launches the agentic workflow.
     *
     * @return the id of the launched execution
     */
    @POST
    @Path("generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AiGenerateResponse generate(AiGenerateRequest request) {
         File apDirectory = requireOpenedAutomationPackage();

        IDEAiConfiguration aiConfiguration = IDEAiConfiguration.from(LocalIDEState.get().getConfiguration());
        String unavailabilityReason = aiConfiguration.unavailabilityReason();
        if (unavailabilityReason != null) {
            throw new ControllerServiceException(412, unavailabilityReason);
        }

        List<AiTestCaseInput> testCases = normalizeTestCases(request);
        AutomationPackageSpecStore specStore = specStore();
        for (AiTestCaseInput testCase : testCases) {
            try {
                specStore.write(testCase.name(), testCase.spec());
            } catch (IllegalArgumentException e) {
                throw badRequest(e.getMessage());
            } catch (IOException e) {
                logger.error("Unable to write the spec of test case {}", testCase.name(), e);
                throw new ControllerServiceException(500, e.getMessage());
            }
        }

        ExecutionParameters executionParameters = buildExecutionParameters(aiConfiguration, apDirectory, testCases, request.hints());
        String executionId = LocalIDEState.get().executeAutomationPackage(new IDEExecutionRequest(
            aiConfiguration.agentPackage().toPath(), executionParameters, List.of(aiConfiguration.agentPlanName())));
        return new AiGenerateResponse(executionId);
    }

    /**
     * Brings both dialog modes onto the same shape: the structured form provides the test cases directly, the free
     * text mode provides a markdown document delimited by level 2 headings.
     */
    private List<AiTestCaseInput> normalizeTestCases(AiGenerateRequest request) {
        boolean hasTestCases = request != null && request.testCases() != null && !request.testCases().isEmpty();
        boolean hasSpecText = request != null && request.specText() != null && !request.specText().isBlank();

        if (hasTestCases == hasSpecText) {
            throw badRequest("Provide either a list of test cases or a specification text, but not both");
        }

        List<AiTestCaseInput> testCases = new ArrayList<>();
        if (hasTestCases) {
            for (AiTestCaseInput testCase : request.testCases()) {
                if (testCase.spec() == null || testCase.spec().isBlank()) {
                    throw badRequest("The specification of test case '" + testCase.name() + "' must not be empty");
                }
                testCases.add(new AiTestCaseInput(testCase.name(), testCase.spec(), normalizeMode(testCase.mode()), testCase.hints()));
            }
        } else {
            try {
                specMarkdownParser.parse(request.specText())
                    .forEach(parsed -> testCases.add(new AiTestCaseInput(parsed.name(), parsed.spec(), MODE_CREATE, null)));
            } catch (IllegalArgumentException e) {
                throw badRequest(e.getMessage());
            }
        }

        Set<String> names = new HashSet<>();
        for (AiTestCaseInput testCase : testCases) {
            if (testCase.name() == null || testCase.name().isBlank()) {
                throw badRequest("Every test case must have a name");
            }
            if (!names.add(testCase.name())) {
                throw badRequest("Duplicate test case name: " + testCase.name());
            }
        }
        return testCases;
    }

    private String normalizeMode(String mode) {
        return MODE_REGENERATE.equalsIgnoreCase(mode) ? MODE_REGENERATE : MODE_CREATE;
    }

    private ExecutionParameters buildExecutionParameters(IDEAiConfiguration aiConfiguration, File apDirectory,
                                                         List<AiTestCaseInput> testCases, String hints) {
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(PARAM_TARGET_DIRECTORY, canonicalPath(apDirectory));
        try {
            customParameters.put(PARAM_INPUT, objectMapper.writeValueAsString(new AiInput(testCases, hints)));
        } catch (Exception e) {
            throw new ControllerServiceException(500, "Unable to serialize the test cases: " + e.getMessage());
        }

        ExecutionParameters executionParameters = new ExecutionParameters();
        executionParameters.setMode(ExecutionMode.RUN);
        executionParameters.setDescription(describe(testCases));
        executionParameters.setCustomParameters(customParameters);
        return executionParameters;
    }

    private String describe(List<AiTestCaseInput> testCases) {
        if (testCases.size() == 1) {
            AiTestCaseInput testCase = testCases.get(0);
            return (MODE_REGENERATE.equals(testCase.mode()) ? "AI regeneration of " : "AI generation of ") + testCase.name();
        }
        return "AI generation of " + testCases.size() + " test cases";
    }

    private String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * The global {@link step.core.controller.errorhandling.ErrorFilter} turns anything that is not a
     * {@link ControllerServiceException} into a 500, so user errors have to be reported through it to reach the
     * frontend with a usable status code.
     */
    private ControllerServiceException badRequest(String message) {
        return new ControllerServiceException(400, message);
    }

    private AutomationPackageSpecStore specStore() {
        return new AutomationPackageSpecStore(requireOpenedAutomationPackage());
    }

    private File requireOpenedAutomationPackage() {
        try {
            return LocalIDEState.get().requireCurrentAutomationPackageDirectory().toFile();
        } catch (IllegalStateException e) {
            throw new ControllerServiceException(409, e.getMessage());
        }
    }
}
