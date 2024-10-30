/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.cli;

import org.junit.Test;
import org.mockito.Mockito;
import step.client.controller.ControllerServicesClient;
import step.core.Version;

import static org.junit.Assert.*;

public class ControllerVersionValidatorTest {

    @Test
    public void compareVersions() {
        ControllerVersionValidator.Result result = ControllerVersionValidator.compareVersions(new Version(1, 1, 0), new Version(1, 1, 0));
        assertEquals(ControllerVersionValidator.Status.EQUAL, result.getStatus());
        result = ControllerVersionValidator.compareVersions(new Version(1, 2, 0), new Version(1, 1, 0));
        assertEquals(ControllerVersionValidator.Status.MAJOR_MISMATCH, result.getStatus());
        assertEquals(new Version(1, 1, 0).toString(), result.getServerVersion().toString());
        assertEquals(new Version(1, 2, 0).toString(), result.getClientVersion().toString());
        result = ControllerVersionValidator.compareVersions(new Version(1, 1, 1), new Version(1, 1, 0));
        assertEquals(ControllerVersionValidator.Status.MINOR_MISMATCH, result.getStatus());
    }

    @Test
    public void validateVersions() throws ControllerVersionValidator.ValidationException {
        ControllerServicesClient mock = Mockito.mock(ControllerServicesClient.class);
        Mockito.when(mock.getControllerVersion()).thenReturn(new Version(1, 1, 0));
        ControllerVersionValidator controllerVersionValidator = new ControllerVersionValidator(mock);
        ControllerVersionValidator.Result result = controllerVersionValidator.validateVersions(new Version(1, 1, 0));
        assertEquals(ControllerVersionValidator.Status.EQUAL, result.getStatus());
        assertEquals(new Version(1, 1, 0).toString(), result.getServerVersion().toString());
        assertThrows(ControllerVersionValidator.ValidationException.class, () -> controllerVersionValidator.validateVersions(new Version(1, 2, 0)));
        assertThrows(ControllerVersionValidator.ValidationException.class, () -> controllerVersionValidator.validateVersions(new Version(2, 1, 0)));
    }
}