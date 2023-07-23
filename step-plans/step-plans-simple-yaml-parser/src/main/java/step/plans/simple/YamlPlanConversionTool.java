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
package step.plans.simple;

import step.core.plans.Plan;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;

import java.io.*;

public class YamlPlanConversionTool {

    public static void main(String[] args) {
        String folder = args.length > 0 ? args[0] : null;
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
            writer.write("\n ---- STEP PLAN CONVERSION TOOL - STARTED ----\n");
            YamlPlanSerializer yamlPlanSerializer = new YamlPlanSerializer();
            PlanParser plainTextPlanParser = new PlanParser();

            if (folder == null || folder.isEmpty()) {
                writer.write("ERROR: Input folder is not specified in program arguments\n");
                return;
            }
            File inputFolder = new File(folder);
            if (!inputFolder.isDirectory()) {
                writer.write("ERROR: " + folder + " is not a directory\n");
                return;
            }

            File outputFolder = new File(inputFolder, "out");
            if (!outputFolder.exists()) {
                boolean outputFolderOk = outputFolder.mkdir();
                if (!outputFolderOk) {
                    writer.write("ERROR: " + outputFolder.getAbsolutePath() + " is not created\n");
                    return;
                }
            }

            writer.write("Converting plans in " + inputFolder.getAbsolutePath() + " ...\n");

            File[] fullPlans = inputFolder.listFiles((dir, name) -> name.matches(".*\\.plan"));
            if (fullPlans == null || fullPlans.length == 0) {
                writer.write("No plans have been found\n");
                return;
            }
            writer.write(fullPlans.length + " plan(s) will be converted to the simple format\n");

            for (File fullPlan : fullPlans) {
                File outFile = new File(outputFolder, fullPlan.getName() + ".yml");
                try (FileInputStream fis = new FileInputStream(fullPlan); FileOutputStream fos = new FileOutputStream(outFile)) {
                    writer.write("Converting " + fullPlan.getName() + " ...\n");

                    // read plan from plain text format
                    // TODO: always TestCase?
                    Plan planFromPlainText = plainTextPlanParser.parse(fis, RootArtefactType.TestCase);

                    // convert to simple yaml and save in output file
                    yamlPlanSerializer.writePlanInSimpleFormat(fos, planFromPlainText);

                    writer.write("SUCCESS: " + outFile.getAbsolutePath() + "\n");
                } catch (Exception e) {
                    writer.write("ERROR: " + fullPlan.getName() + " hasn't been converted!\n");
                    e.printStackTrace();
                }
            }
            writer.write("---- STEP PLAN CONVERSION TOOL - FINISHED ----\n");
        } catch (IOException e) {
            System.out.println("ERROR: Internal error");
            e.printStackTrace();
        }
    }
}
