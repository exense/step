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
package step.core.artefacts.reports.junitxml.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.checkerframework.checker.units.qual.A;

@XmlRootElement(name = "testSuite")
public class TestSuite {
    private String name;
    private String time;

    private Integer tests = 0;
    private Integer errors = 0;
    private Integer skipped = 0;
    private Integer failures = 0;

    public TestSuite() {
    }

    public String getName() {
        return name;
    }

    @XmlAttribute
    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    @XmlAttribute
    public void setTime(String time) {
        this.time = time;
    }

    public Integer getTests() {
        return tests;
    }

    @XmlAttribute
    public void setTests(Integer tests) {
        this.tests = tests;
    }

    public Integer getErrors() {
        return errors;
    }

    @XmlAttribute
    public void setErrors(Integer errors) {
        this.errors = errors;
    }

    public Integer getSkipped() {
        return skipped;
    }

    @XmlAttribute
    public void setSkipped(Integer skipped) {
        this.skipped = skipped;
    }

    public Integer getFailures() {
        return failures;
    }

    @XmlAttribute
    public void setFailures(Integer failures) {
        this.failures = failures;
    }
}
