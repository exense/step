/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.ApFileNames;
import step.core.AbstractContext;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.java.handler.GeneralScriptHandler;
import step.plugins.js223.handler.ScriptHandler;
import step.resources.Resource;
import step.resources.ResourceManager;

import static step.resources.ResourceManager.RESOURCE_TYPE_FUNCTIONS;

public abstract class AbstractScriptFunctionType<T extends GeneralScriptFunction> extends AbstractFunctionType<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractScriptFunctionType.class);

    /**
     * The templates are part of the controller distribution, which the Step IDE is not. A copy travels
     * with this plugin so that a keyword created from the editor gets a script with something in it.
     */
    private static final String BUNDLED_TEMPLATES_PATH = "templates/";

    protected Configuration configuration;

    public AbstractScriptFunctionType(Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    @Override
    public void init() {
        super.init();
        handlerPackageVersion = registerResource(getClass().getClassLoader(), "java-plugin-handler.jar", false, false);
    }

    @Override
    public HandlerProperties getHandlerProperties(T function, AbstractStepContext executionContext) {
        HandlerProperties handlerProperties = super.getHandlerProperties(function, executionContext);
        Map<String, String> props = new HashMap<>();
        List<AutoCloseable> createdCloseables = new ArrayList<>();
        try {
            props.put(ScriptHandler.SCRIPT_LANGUAGE, function.getScriptLanguage().get());

            createdCloseables.add(registerFile(function.getLibrariesFile(), ScriptHandler.LIBRARIES_FILE, props, true, executionContext));
            createdCloseables.add(addPluginLibsIfRequired(function.getScriptLanguage().get(), props));
            createdCloseables.add(registerFile(function.getScriptFile(), ScriptHandler.SCRIPT_FILE, props, true, executionContext));
            createdCloseables.add(registerFile(function.getErrorHandlerFile(), ScriptHandler.ERROR_HANDLER_FILE, props, true, executionContext));
            if (configuration.getPropertyAsBoolean("plugins.java.validate.properties")) {
                props.put(KeywordExecutor.VALIDATE_PROPERTIES, Boolean.TRUE.toString());
            }

            return handlerProperties.merge(props, createdCloseables);
        } catch (Throwable e) {
            handlerProperties.close();
            closeRegisteredCloseable(createdCloseables);
            throw e;
        }
    }

    protected AutoCloseable addPluginLibsIfRequired(String scriptLanguage, Map<String, String> props) {
        String property = configuration.getProperty("plugins." + scriptLanguage + ".libs", null);
        if (property != null) {
            return registerFile(new File(property), ScriptHandler.PLUGIN_LIBRARIES_FILE, props, true);
        }
        return null;
    }

    @Override
    public String getHandlerChain(GeneralScriptFunction function) {
        return GeneralScriptHandler.class.getName();
    }

    public static final Map<String, String> fileExtensionMap = new ConcurrentHashMap<>();

    {
        fileExtensionMap.put("groovy", "groovy");
        fileExtensionMap.put("python", "py");
        fileExtensionMap.put("javascript", "js");
    }

    protected File getDefaultScriptFile(GeneralScriptFunction function, String scriptDir) {
        String filename = getScriptFilename(function);
        File file = new File(scriptDir + "/" + filename);
        return file;
    }

    private String getScriptFilename(GeneralScriptFunction function) {
        StringBuilder filename = new StringBuilder();
        if (function.getAttributes().containsKey(AbstractOrganizableObject.NAME)) {
            filename.append(function.getAttributes().get(AbstractOrganizableObject.NAME));
            filename.append("_");
        }
        filename.append(UUID.randomUUID());
        filename.append(".").append(fileExtensionMap.get(getScriptLanguage(function)));
        return filename.toString();
    }

    protected String getScriptLanguage(GeneralScriptFunction conf) {
        return conf.getScriptLanguage().get();
    }

    protected InputStream getTemplateFileInputStream(String templateFilename) throws SetupFunctionException {
        if (templateFilename == null) {
            return null;
        }
        File templateScript = new File(configuration.getProperty("controller.dir") + "/data/templates/" + templateFilename);
        if (templateScript.exists()) {
            try {
                return new FileInputStream(templateScript);
            } catch (FileNotFoundException e) {
                throw new SetupFunctionException("Unable to apply template. The file '" + templateScript.getAbsolutePath() + "' doesn't exist");
            }
        }
        // The distribution's template takes precedence - it is the one an administrator can edit - and the
        // bundled copy takes over where there is no controller directory to hold one, i.e. in the IDE.
        InputStream bundledTemplate = AbstractScriptFunctionType.class.getResourceAsStream(BUNDLED_TEMPLATES_PATH + templateFilename);
        if (bundledTemplate == null) {
            log.warn("Default template file not found: " + templateScript.getAbsolutePath());
        }
        return bundledTemplate;
    }

    protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
        return setupScriptFile(function, templateStream, configuration.getProperty("keywords.script.scriptdir"));
    }

    /**
     * @param templateStream the content of the script to create, {@code null} for an empty one.
     *                       <b>Closed by this method</b>, whether it is read or not - a keyword created
     *                       against a script that exists already leaves it unread
     */
    protected File setupScriptFile(GeneralScriptFunction function, InputStream templateStream,
                                   String scriptDir) throws SetupFunctionException {
        try (InputStream template = templateStream) {
            File scriptFile;

            Path automationPackageRoot = getEditableAutomationPackageRoot();
            if (automationPackageRoot != null) {
                // automationPackageRoot is only set in the editor, and the script goes there rather than in
                // scriptDir, which is a directory of a controller installation
                return apScriptFileWriter(automationPackageRoot, function).create(function, scriptFileExtension(function), template);
            }

            String scriptFilename = function.getScriptFile().get();

            // The keyword was created against a script that already exists - a Step resource or a file of an
            // automation package - so there is nothing to set up. Both are resolved on the fly at execution
            // time; taking the reference for a path would create a file literally named "resource:<id>".
            if (FileResolver.isResource(scriptFilename) || FileResolver.isApResource(scriptFilename)) {
                return null;
            }

            if (scriptFilename == null || scriptFilename.trim().length() == 0) {
                scriptFile = getDefaultScriptFile(function, scriptDir);
                function.getScriptFile().setValue(scriptFile.getAbsolutePath());
            } else {
                scriptFile = new File(scriptFilename);
            }

            if (!scriptFile.exists()) {
                File folder = scriptFile.getParentFile();
                if (!folder.exists()) {
                    try {
                        Files.createDirectory(folder.toPath());
                    } catch (IOException e) {
                        throw new SetupFunctionException("Unable to create script folder '" + folder.getAbsolutePath() + "' for function '" + function.getAttributes().get(AbstractOrganizableObject.NAME), e);
                    }
                }
                try {
                    scriptFile.createNewFile();
                } catch (IOException e) {
                    throw new SetupFunctionException("Unable to create script folder '" + folder.getAbsolutePath() + "' for function '" + function.getAttributes().get(AbstractOrganizableObject.NAME), e);
                }

                if (template != null) {
                    applyTemplate(scriptFile, template);
                }
            }

            return scriptFile;
        } catch (IOException e) {
            throw new SetupFunctionException("Unable to read the template of the script of function '"
                + function.getAttributes().get(AbstractOrganizableObject.NAME) + "'", e);
        }
    }

    protected File setupScriptFileAsResource(GeneralScriptFunction function, String templateFilename) throws SetupFunctionException {
        return setupScriptFileAsResource(function, getTemplateFileInputStream(templateFilename));
    }

    /**
     * @return the root directory of the automation package open in the editor, or {@code null} on a Step
     * server, where automation packages are immutable archives. The {@link FileResolver} is the only
     * dependency a function type has that knows about them, which is why the question is asked of it.
     * @throws IllegalStateException if the editor has no automation package open
     */
    protected Path getEditableAutomationPackageRoot() {
        return fileResolver == null ? null : fileResolver.getEditableApRoot();
    }

    /**
     * @return the writer creating the script files of the automation package open in the editor
     */
    protected ApScriptFileWriter apScriptFileWriter(Path automationPackageRoot, GeneralScriptFunction function) {
        return new ApScriptFileWriter(automationPackageRoot, getApScriptDirectory(function));
    }

    /**
     * Where the editor places the script it generates for a keyword, relative to the package root: one
     * directory per language - {@code groovy}, {@code javascript} - so that a package's sources are sorted
     * by what they are, and so that nothing lands in {@code keywords}, which is where the YAML fragment of
     * a keyword goes (see {@code AutomationPackageYamlFragmentManager}).
     * <p>
     * A function type whose scripts are more than their language overrides this - EE's Oryon keywords go
     * to {@code oryon} rather than being mixed into the plain Groovy ones.
     *
     * @see ApFileNames#sanitize(String) applied because the language is a value the user can set
     */
    protected String getApScriptDirectory(GeneralScriptFunction function) {
        return ApFileNames.sanitize(getScriptLanguage(function));
    }

    protected String scriptFileExtension(GeneralScriptFunction function) {
        return fileExtensionMap.get(getScriptLanguage(function));
    }

    /**
     * @param templateStream the content of the script to create, {@code null} for an empty one.
     *                       <b>Closed by this method</b>, whether it is read or not
     */
    protected File setupScriptFileAsResource(GeneralScriptFunction function, InputStream templateStream) throws SetupFunctionException {
        try (InputStream template = templateStream) {
            Path automationPackageRoot = getEditableAutomationPackageRoot();
            if (automationPackageRoot != null) {
                return apScriptFileWriter(automationPackageRoot, function).create(function, scriptFileExtension(function), template);
            }

            ResourceManager resourceManager = fileResolver.getResourceManager();
            String newScriptFilename = getScriptFilename(function);
            InputStream resourceIS = Objects.requireNonNullElse(template, InputStream.nullInputStream());
            // apply context attributes of the function package to the function
            AbstractContext context = new AbstractContext() {
            };
            objectHookRegistry.rebuildContext(context, function);
            ObjectEnricher objectEnricher = objectHookRegistry.getObjectEnricher(context);
            Resource resource = resourceManager.createResource(RESOURCE_TYPE_FUNCTIONS, resourceIS, newScriptFilename, objectEnricher, null);
            function.getScriptFile().setValue(fileResolver.createPathForResourceId(resource.getId().toHexString()));
            return fileResolver.resolve(function.getScriptFile().get());
        } catch (SetupFunctionException e) {
            throw e;
        } catch (Exception e) {
            throw new SetupFunctionException("Unable to create the default script as resource", e);
        }
    }

    @Override
    public T copyFunction(T function) throws FunctionTypeException {
        T copy = super.copyFunction(function);
        DynamicValue<String> scriptFile = function.getScriptFile();//copy of the source script file
        File newFile = null;
        if (function.getScriptLanguage().get().equals("groovy") || function.getScriptLanguage().get().equals("javascript")) {
            Path automationPackageRoot = getEditableAutomationPackageRoot();
            if (automationPackageRoot != null) {
                copy.setScriptFile(new DynamicValue<>(apScriptFileWriter(automationPackageRoot, copy)
                    .copy(copy, scriptFileExtension(copy), scriptFile.get(), fileResolver)));
                return copy;
            }
            try {
                copy.setScriptFile(new DynamicValue<>(""));//reset script to setup a new one
                String scriptFileValue = scriptFile.get();

                // Both resource: and apResource: scripts must be resolved and re-created as a
                // standalone resource, so the copy is detached from the original (an apResource: copy
                // left as-is would stay bound to the source automation package's lifecycle).
                boolean isResource = FileResolver.isResource(scriptFileValue) || FileResolver.isApResource(scriptFileValue);
                if (isResource) {
                    scriptFileValue = fileResolver.resolve(scriptFileValue).getAbsolutePath();
                    newFile = setupScriptFileAsResource(copy, new FileInputStream(scriptFileValue));
                } else {
                    String parent = null;
                    try {
                        parent = new File(scriptFileValue).getParent();
                    } catch (Exception e) {
                        //keep configuration script dir in case of error
                    }
                    newFile = (parent != null) ?
                        setupScriptFile(copy, new FileInputStream(scriptFileValue), parent) :
                        setupScriptFile(copy, new FileInputStream(scriptFileValue));
                }
            } catch (SetupFunctionException | FileNotFoundException e) {
                //Keep source config in case of error
            } finally {
                if (newFile == null) {
                    copy.setScriptFile(scriptFile);
                }
            }
        }
        return copy;
    }

    private void applyTemplate(File scriptFile, InputStream templateScript) throws SetupFunctionException {
        if (templateScript != null) {
            try {
                Files.copy(templateScript, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new SetupFunctionException("Unable to copy template from stream to '" + scriptFile.getAbsolutePath() + "'", e);
            }
        } else {
            throw new SetupFunctionException("Unable to apply template. The stream is null");
        }
    }

    public File getScriptFile(T function) {
        String scriptFilePath = function.getScriptFile().get();
        return fileResolver.resolve(scriptFilePath);
    }
}
