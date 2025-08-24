/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */

package step.resources;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;

import static org.junit.Assert.*;
public class ResourceTest {

    @Test
    public void copy() {
        Resource resource = new Resource("oldActor");
        resource.addAttribute("name", "myResourceName");
        resource.addCustomField("myCustomKey", "myCustomValue");
        resource.setOrigin(":uploaded");

        Date timeBeforeCopy = new Date();
        Resource copy = resource.copy("newActor");
        Assert.assertEquals("newActor", copy.getCreationUser());
        Assert.assertEquals("newActor", copy.getLastModificationUser());
        Assert.assertTrue(
                "Resource creation date: " + copy.getCreationDate().toInstant() + "; Time before copy: " + timeBeforeCopy.toInstant(),
                !copy.getCreationDate().toInstant().isBefore(timeBeforeCopy.toInstant())
        );
        Assert.assertTrue(
                "Resource modification date: " + copy.getLastModificationDate().toInstant() + "; Time before copy: " + timeBeforeCopy.toInstant(),
                !copy.getLastModificationDate().toInstant().isBefore(timeBeforeCopy.toInstant())
        );

        Assert.assertEquals(resource.getId(), copy.getId());
        Assert.assertEquals(":uploaded", copy.getOrigin());
        Assert.assertEquals("myCustomValue", copy.getCustomField("myCustomKey"));
        Assert.assertEquals("myResourceName", copy.getAttribute("name"));
    }
}