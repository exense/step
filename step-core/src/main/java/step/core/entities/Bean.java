package step.core.entities;

import org.json.JSONObject;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.serialization.DottedKeyMap;

import javax.json.JsonObject;

public class Bean extends AbstractOrganizableObject {

    private String property1;

    private JsonObject jsonObject;

    private JSONObject jsonOrgObject;

    private DottedKeyMap<String, String> map;

    public Bean() {
        super();
    }

    public Bean(String property1) {
        super();
        this.property1 = property1;
    }

    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public DottedKeyMap<String, String> getMap() {
        return map;
    }

    public void setMap(DottedKeyMap<String, String> map) {
        this.map = map;
    }

    public void setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getJsonOrgObject() {
        return jsonOrgObject;
    }

    public void setJsonOrgObject(JSONObject jsonOrgObject) {
        this.jsonOrgObject = jsonOrgObject;
    }

    @Override
    public String toString() {
        return this.getId().toHexString();
    }
}


