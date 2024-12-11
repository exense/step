package step.functions.handler;

import ch.exense.commons.io.FileHelper;
import ch.exense.commons.processes.ManagedProcess;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.functions.handler.json.JsonObjectDeserializer;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class ProcessBasedFunctionHandler extends JsonBasedFunctionHandler {

    private static final String KEYWORD_INPUT_PROPERTIES_FILE = "step_keyword_input.properties";
    private static final String KEYWORD_OUTPUT_PROPERTIES_FILE = "step_keyword_output.properties";
    private static final String KEYWORD_INPUT_JSON_FILE = "step_keyword_input_message.json";
    private static final String KEYWORD_OUTPUT_JSON_FILE = "step_keyword_output_message.json";
    public static final String DEBUG = "debug";
    public static final String ATTACH_WORK_FOLDER = "attachWorkFolder";

    private static final Logger logger = LoggerFactory.getLogger(ProcessBasedFunctionHandler.class);

    abstract protected List<String> getProcessCommand(Input<JsonObject> input) throws Exception;
    abstract protected String getProcessName();

    @Override
    public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
        Map<String, String> properties = input.getProperties();
        boolean debug = Boolean.parseBoolean(properties.getOrDefault(DEBUG, Boolean.FALSE.toString()));
        boolean attachWorkFolder = Boolean.parseBoolean(properties.getOrDefault(ATTACH_WORK_FOLDER, Boolean.FALSE.toString()));

        List<String> command = getProcessCommand(input);
        String processName = getProcessName();
        try(ManagedProcess process = new ManagedProcess(processName, command)) {
            createProcessInputMessageAsJson(process, input);
            createProcessInputAsProperties(process, input);

            process.start();
            int returnCode = process.waitFor((long) (input.getFunctionCallTimeout() * 0.9));

            OutputBuilder outputBuilder = processOutput(process);

            boolean error = false;
            if (returnCode != 0) {
                error = true;
                outputBuilder.appendError("The " + getProcessName() + " execution returned " + returnCode + ". Check logs for more details.");
            }

            if (error || debug) {
                attachProcessLogs(process, outputBuilder);
                takeScreenshot(outputBuilder);
            }

            if (attachWorkFolder) {
                attachProcessWorkFolder(process, outputBuilder);
            }

            return outputBuilder.build();
        }
    }

    protected String getSanitizedProcessName() {
        // Remove leading/trailing whitespace and replace spaces with underscores
        String sanitized = getProcessName().trim().replaceAll("\\s+", "_");
        // Use replaceAll() to remove invalid characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_.]", "_");
        return sanitized;
    }

    protected static void createProcessInputAsProperties(ManagedProcess process, Input<?> input) throws IOException {
        //Create input properties file from keyword properties and keyword arguments
        Map<String, String> argumentMap = new HashMap<>();
        Map<String, String> properties = input.getProperties();
        if (properties != null) {
            properties.forEach((k, v) -> argumentMap.put(k, v));
        }
        // Inputs have precedence over properties
        JsonObject inputJson = (JsonObject) input.getPayload();
        if (inputJson != null) {
            for (String key : inputJson.keySet()) {
                argumentMap.put(key, inputJson.get(key).toString());
            }
        }
        Properties keywordProperties = new Properties();
        keywordProperties.putAll(argumentMap);
        // Write properties to file
        File stepKeywordPropertiesFile = new File(process.getExecutionDirectory(), KEYWORD_INPUT_PROPERTIES_FILE);
        try (FileOutputStream outputStream = new FileOutputStream(stepKeywordPropertiesFile)) {
            keywordProperties.store(outputStream, "Keyword properties");
        }
    }

    protected static void createProcessInputMessageAsJson(ManagedProcess process, Input<?> input) throws IOException {
        //Create input properties file from keyword properties and keyword arguments
        JsonObjectBuilder outputMessageBuilder = Json.createObjectBuilder();
        outputMessageBuilder.add("inputs", (JsonObject)  input.getPayload());
        JsonObjectBuilder propertiesJsonObject = Json.createObjectBuilder();
        input.getProperties().forEach(propertiesJsonObject::add);
        outputMessageBuilder.add("properties", propertiesJsonObject);

        // Write properties to file
        File stepKeywordPropertiesFile = new File(process.getExecutionDirectory(), KEYWORD_INPUT_JSON_FILE);
        try (FileOutputStream outputStream = new FileOutputStream(stepKeywordPropertiesFile);
             JsonWriter jsonWriter = Json.createWriter(outputStream)) {
            // Write the JsonObject to the file
            jsonWriter.writeObject(outputMessageBuilder.build());
        }
    }

    protected OutputBuilder processOutput(ManagedProcess process) {
        File stepKeywordJsonFile = new File(process.getExecutionDirectory(), KEYWORD_OUTPUT_JSON_FILE);
        File stepKeywordPropertiesFile = new File(process.getExecutionDirectory(), KEYWORD_OUTPUT_PROPERTIES_FILE);
        if (stepKeywordJsonFile.exists() && stepKeywordJsonFile.isFile()) {
            return processOutputAsJson(process, stepKeywordJsonFile);
        } else if (stepKeywordPropertiesFile.exists() && stepKeywordPropertiesFile.isFile()) {
            return processOutputAsProperties(process, stepKeywordPropertiesFile);
        } else {
            return new OutputBuilder();
        }
    }

    protected OutputBuilder processOutputAsProperties(ManagedProcess process, File stepKeywordPropertiesFile) {
        OutputBuilder outputBuilder = new OutputBuilder();
        if (stepKeywordPropertiesFile.exists() && stepKeywordPropertiesFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(stepKeywordPropertiesFile)) {
                Properties outputProperties = new Properties();
                outputProperties.load(fis);
                outputProperties.keySet().stream().map(Object::toString).forEach(k -> outputBuilder.add(k,outputProperties.getProperty(k)));
            } catch (IOException e) {
                logger.error("Error while reading file " + KEYWORD_OUTPUT_PROPERTIES_FILE, e);
                outputBuilder.addAttachment(AttachmentHelper.generateAttachmentForException(e));
                outputBuilder.appendError("Error while reading the Keyword output property file. See agent logs for more details.");
            }
        }
        return outputBuilder;
    }

    protected OutputBuilder processOutputAsJson(ManagedProcess process, File stepKeywordJsonFile) {
        OutputBuilder outputBuilder = new OutputBuilder();
        if (stepKeywordJsonFile.exists() && stepKeywordJsonFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(stepKeywordJsonFile)) {
                TypeReference<Output<JsonObject>> typeRef = new TypeReference<>() {};
                Output<JsonObject> output = JsonObjectDeserializer.getObjectMapper().readValue(stepKeywordJsonFile, typeRef);
                //while we deserialize directly as Output we still return an output builder so that the caller can still enrich it with more data
                if (output.getError() != null) {
                    outputBuilder.setError(output.getError());
                }
                if (output.getPayload() != null) {
                    JsonObjectBuilder payloadBuilder = outputBuilder.getPayloadBuilder();
                    output.getPayload().forEach(payloadBuilder::add);
                }
                if (output.getMeasures() != null) {
                    output.getMeasures().forEach(m -> outputBuilder.addMeasure(m.getName(), m.getDuration(), m.getBegin(), m.getData()));
                }
                if (output.getAttachments() != null) {
                    output.getAttachments().forEach(outputBuilder::addAttachment);
                }
            } catch (Exception e) {
                logger.error("Error while reading file " + KEYWORD_OUTPUT_JSON_FILE, e);
                outputBuilder.addAttachment(AttachmentHelper.generateAttachmentForException(e));
                outputBuilder.appendError("Error while reading the Keyword output property file. See agent logs for more details.");
            }
        }
        return outputBuilder;
    }

    protected void attachProcessLogs(ManagedProcess process, OutputBuilder outputBuilder) {
        String processOutput = getProcessOutput(process);
        Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(processOutput.getBytes(StandardCharsets.UTF_8), getSanitizedProcessName() + ".log");
        outputBuilder.addAttachment(attachment);
        String processErrorOutput = getProcessErrorOutput(process);
        if (processErrorOutput != null && !processErrorOutput.isBlank()) {
            Attachment errorAttachment = AttachmentHelper.generateAttachmentFromByteArray(processErrorOutput.getBytes(StandardCharsets.UTF_8), getSanitizedProcessName() + "Error.log");
            outputBuilder.addAttachment(errorAttachment);
        }
    }

    protected void attachProcessWorkFolder(ManagedProcess process, OutputBuilder outputBuilder) {
        File executionDirectory = process.getExecutionDirectory();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (executionDirectory.exists() && executionDirectory.isDirectory()) {
            try {
                FileHelper.zip(executionDirectory, outputStream);
                outputBuilder.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(outputStream.toByteArray(),"workFolder.zip"));
            } catch (IOException e) {
                logger.error("Error while reading creating an archive of the work folder", e);
                outputBuilder.addAttachment(AttachmentHelper.generateAttachmentForException(e));
                outputBuilder.appendError("Error while reading creating an archive of the work folder");
            }
        }
    }

    private String getProcessErrorOutput(ManagedProcess process) {
        return readFile(process.getProcessErrorLog());
    }

    private String getProcessOutput(ManagedProcess process) {
        return readFile(process.getProcessOutputLog());
    }

    private String readFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            logger.error("Error while reading file {}", file, e);
            return "Error while reading process output. See agent logs for more details.";
        }
    }

    protected void takeScreenshot(OutputBuilder outputBuilder) throws AWTException, IOException {
        Robot robot = new Robot();
        BufferedImage screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(screenShot, "PNG", byteArrayOutputStream);
        outputBuilder.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(byteArrayOutputStream.toByteArray(), "screenshot.png"));
    }
}
