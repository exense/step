package step.core.entities;

import javax.json.JsonObject;

import org.json.JSONObject;

import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.serialization.DottedKeyMap;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public class Bean extends AbstractOrganizableObject {

    private String property1;
    private Long longProperty;
    private boolean booleanProperty;

    private JsonObject jsonObject;

    private JSONObject jsonOrgObject;

    private DottedKeyMap<String, String> map;

    private Bean nested;

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

    public Long getLongProperty() {
		return longProperty;
	}

	public void setLongProperty(Long longProperty) {
		this.longProperty = longProperty;
	}

	public boolean isBooleanProperty() {
		return booleanProperty;
	}

	public void setBooleanProperty(boolean booleanProperty) {
		this.booleanProperty = booleanProperty;
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

    public Bean getNested() {
        return nested;
    }

    public void setNested(Bean nested) {
        this.nested = nested;
    }

    @Override
    public String toString() {
        return this.getId().toHexString();
    }
}


