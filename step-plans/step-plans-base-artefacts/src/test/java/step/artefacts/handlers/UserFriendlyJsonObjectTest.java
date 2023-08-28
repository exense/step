package step.artefacts.handlers;

import com.google.api.client.util.Sets;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.apache.commons.compress.utils.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UserFriendlyJsonObjectTest {

    @Test
    public void testUnwrapping(){
        JsonProvider provider = JsonProvider.provider();
        JsonObjectBuilder builder = provider.createObjectBuilder();
        builder.add("intKey", 77);
        builder.add("boolKey", true);
        builder.add("doubleKey", 777.77);
        builder.add("stringKey", "testString");
        builder.add("arrayKey", provider.createArrayBuilder().add("a").add("b").add("c").build());
        builder.add("nestedObject", provider.createObjectBuilder().add("nestedKey1", "n1").add("nestedKey2", "n2"));
        UserFriendlyJsonObject ufJson = new UserFriendlyJsonObject(builder.build());

        HashSet<Object> expected = Sets.newHashSet();
        expected.add("intKey");
        expected.add("boolKey");
        expected.add("doubleKey");
        expected.add("stringKey");
        expected.add("arrayKey");
        expected.add("nestedObject");
        assertEquals(expected, ufJson.keySet());

        assertEquals("testString", ufJson.get("stringKey"));
        assertEquals(true, ufJson.get("boolKey"));
        ArrayList<Object> expectedList = Lists.newArrayList();
        expectedList.add("a");
        expectedList.add("b");
        expectedList.add("c");
        assertEquals(expectedList, ufJson.get("arrayKey"));
        assertEquals(77, ufJson.get("intKey"));
        assertEquals(777.77, ufJson.get("doubleKey"));
        assertEquals("n2", ((Map<String, Object>) ufJson.get("nestedObject")).get("nestedKey2"));
    }
}