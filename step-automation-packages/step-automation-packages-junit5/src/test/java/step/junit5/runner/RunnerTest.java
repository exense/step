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
package step.junit5.runner;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.List;

public class RunnerTest {
    @Test
    public void runTest(){
        Assertions.assertTrue(true);
    }

    @TestFactory
    public List<DynamicNode> dynamicTests(){
        DynamicTest test = DynamicTest.dynamicTest("First dynamic test", () -> Assertions.assertTrue(true));
        DynamicTest test2 = DynamicTest.dynamicTest("Second dynamic test", () -> Assertions.assertTrue(true));
        List<DynamicNode> testNodes = Arrays.asList(test, test2);
        return List.of(DynamicContainer.dynamicContainer("Parent dynamic node", testNodes.stream()));
    }

    @Nested
    class NestedTests {
        @Test
        public void nestedA(){
            Assertions.assertTrue(true);
        }

        @Test
        public void nestedB(){
            Assertions.assertTrue(true);
        }
    }
}
