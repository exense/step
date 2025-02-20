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
package step.plans.parser.yaml.editor;

import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.plans.PlanCompilationError;
import step.core.plans.PlanCompiler;
import step.core.plans.PlanCompilerException;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

class YamlEditorPlanTypeCompiler implements PlanCompiler<YamlEditorPlan> {

    private static final Logger log = LoggerFactory.getLogger(YamlEditorPlanTypeCompiler.class);

    private final YamlPlanReader reader;
    private final LineNumberByJsonPointerResolver lineNumberResolver;

    public YamlEditorPlanTypeCompiler() {
        // TODO: think if we need to configure the actual plan version here (now the default YamlPlanVersions.ACTUAL_VERSION is always used)
        // the reader has 'validateWithJsonSchema'=true
        reader = new YamlPlanReader(null, null, true, null);
        lineNumberResolver = new LineNumberByJsonPointerResolver();
    }

    @Override
    public YamlEditorPlan compile(YamlEditorPlan plan) throws PlanCompilerException {
        String source = plan.getSource();
        Plan parsedPlan;
        if (source == null || source.isEmpty()) {
            return plan;
        }

        try (InputStream is = new ByteArrayInputStream(source.getBytes())) {
            parsedPlan = reader.readYamlPlan(is);
        } catch (YamlPlanValidationException e) {
            PlanCompilerException planCompilerException = new PlanCompilerException();

            // if the reason is validation exception (source doesn't match json schema) we try to resolve the source line
            if (e.getCause() != null && e.getCause() instanceof ValidationException) {
                ValidationException detailedValidationException = (ValidationException) e.getCause();
                List<ValidationException> causingExceptions = detailedValidationException.getCausingExceptions();
                if (causingExceptions != null && !causingExceptions.isEmpty()) {
                    // sometimes the validator provides the causing exceptions, in this case we can collect several error messages with source lines
                    List<LineNumberByJsonPointerResolver.JsonPointerSourceLine> allPointerSourceLines =
                            lineNumberResolver.findLineNumbers(
                                    causingExceptions.stream().map(ValidationException::getPointerToViolation).filter(p -> p != null && !p.isEmpty()).collect(Collectors.toList()), source
                            );

                    for (ValidationException causingException : causingExceptions) {
                        YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
                        compilationError.setMessage(causingException.getErrorMessage());
                        String pointerToViolation = causingException.getPointerToViolation();
                        if (pointerToViolation != null && !pointerToViolation.isEmpty()) {
                            LineNumberByJsonPointerResolver.JsonPointerSourceLine foundLine = allPointerSourceLines.stream().filter(ap -> ap.getJsonPointer().equals(pointerToViolation)).findFirst().orElse(null);
                            if (foundLine != null) {
                                compilationError.setLine(foundLine.getSourceLine());
                            } else {
                                // reference to the first line if the real error line is not resolved
                                compilationError.setLine(1);
                            }
                        }
                        planCompilerException.addError(compilationError);
                    }
                } else {
                    // validator didn't provide the causing errors - in this case we take the main error message only
                    YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
                    compilationError.setMessage(detailedValidationException.getMessage());
                    String pointerToViolation = detailedValidationException.getPointerToViolation();
                    if (pointerToViolation != null && !pointerToViolation.isEmpty()) {
                        compilationError.setLine(lineNumberResolver.findLineNumbers(List.of(pointerToViolation), source).get(0).getSourceLine());
                    }
                    planCompilerException.addError(compilationError);
                }
            } else {
                // non-validation exception
                YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
                compilationError.setMessage(e.getMessage());
                planCompilerException.addError(compilationError);
            }
            throw planCompilerException;
        } catch (Exception e) {
            PlanCompilerException planCompilerException = new PlanCompilerException();
            YamlEditorPlanCompilationError compilationError = new YamlEditorPlanCompilationError();
            compilationError.setMessage(e.getMessage());
            planCompilerException.addError(compilationError);
            throw planCompilerException;
        }

        AbstractArtefact root = plan.getRoot();
        AbstractArtefact parsedRoot = parsedPlan.getRoot();

        // Make sure to keep the same id and attributes for root element
        if (root != null && root.getClass() == parsedRoot.getClass()) {
            parsedRoot.setId(root.getId());
            parsedRoot.setAttributes(root.getAttributes());
        }

        plan.setRoot(parsedRoot);
        plan.setSubPlans(parsedPlan.getSubPlans());
        plan.setFunctions(parsedPlan.getFunctions());

        return plan;
    }

    public String prepareSource(Plan plan) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reader.writeYamlPlan(os, plan);
            return os.toString();
        } catch (IOException e) {
            log.error("Cannot prepare source for plan " + plan.getAttribute(AbstractOrganizableObject.NAME));
            return "";
        }
    }

    static class YamlEditorPlanCompilationError extends PlanCompilationError {

        private int line;

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        @Override
        public String toString() {
            return "YamlEditorPlanCompilationError{" +
                    "message=" + getMessage() + "; " +
                    "line=" + line +
                    '}';
        }
    }


}
