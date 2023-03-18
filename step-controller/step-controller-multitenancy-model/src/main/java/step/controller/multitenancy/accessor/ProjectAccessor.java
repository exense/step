package step.controller.multitenancy.accessor;

import step.controller.multitenancy.model.Project;
import step.core.accessors.Accessor;

import java.util.Map;

public interface ProjectAccessor extends Accessor<Project> {

	Project findByAttributes(Map<String, String> attributes);

}
