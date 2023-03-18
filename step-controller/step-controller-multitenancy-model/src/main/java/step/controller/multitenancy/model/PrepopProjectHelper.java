package step.controller.multitenancy.model;

import org.bson.types.ObjectId;
import step.controller.multitenancy.accessor.ProjectAccessor;

import java.util.HashMap;
import java.util.Map;

public class PrepopProjectHelper {
	
	public static String PROJECT_KEY = "project";

	public enum PrepopProject{
		SYSTEM("system", "admin"),
		COMMON("Common", "admin");
		
		private String name;
		private String owner;
		
		PrepopProject(String name, String owner){
			this.name = name;
			this.owner = owner;
		}
		
		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public String getOwner() {
			return this.owner;
		}

		public void getOwner(String owner) {
			this.owner = owner;
		}
	}
	
	public static void addProjectIdToAttributeMap(Map<String, String> attributes, ProjectAccessor projectAccessor, PrepopProject project) {
		// Project accessor can be null if the multitenancy plugin is disabled
		if(projectAccessor != null) {
			ObjectId projectId = PrepopProjectHelper.resolveProjectId(projectAccessor, project);
			if(projectId != null) {
				attributes.put(PrepopProjectHelper.PROJECT_KEY, projectId.toString());
			}
		}
	}
	
	private static ObjectId resolveProjectId(ProjectAccessor accessor, PrepopProject project) {
		String projectName = project.getName();
		return resolveProjectIdByName(accessor, projectName);
	}

	public static ObjectId resolveProjectIdByName(ProjectAccessor accessor, String projectName) {
		// Project accessor can be null if the multi-tenancy plugin is disabled
		if(accessor != null) {
			Map<String, String> attributes = new HashMap<>();
			attributes.put("name", projectName);
			Project found = accessor.findByAttributes(attributes);
			if(found != null) {
				return found.getId();
			}else {
				return null;
			}
		} else {
			return null;
		}
	}
}
