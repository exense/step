package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Defines the requirements in terms of Agents for a Plan
 */
public class AgentPoolConfiguration {
    public static String TEMPLATE_PROPERTY_NAME = "templateName";
    public static String NUMBER_AGENT_PROPERTY_NAME = "number";
    public static String IMAGE_PROPERTY_NAME = "image";

    public int number;
    public String templateName;
    public String image;

    public AgentPoolConfiguration() {
    }


    public AgentPoolConfiguration(String templateName, String image, int number) {
        this.number = number;
        this.templateName = templateName;
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolConfiguration that = (AgentPoolConfiguration) o;
        return number == that.number && Objects.equals(templateName, that.templateName) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, templateName, image);
    }
}
