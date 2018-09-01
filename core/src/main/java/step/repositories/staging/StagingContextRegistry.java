package step.repositories.staging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.core.plans.Plan;

public class StagingContextRegistry {

	public static class StagingContextImpl {

		protected final String id;
		protected final List<String> attachments = new ArrayList<>();
		protected Plan plan;

		public StagingContextImpl(String id) {
			super();
			this.id = id;
		}

		public boolean addAttachment(String e) {
			return attachments.add(e);
		}

		public Plan getPlan() {
			return plan;
		}

		public void setPlan(Plan plan) {
			this.plan = plan;
		}

		public String getId() {
			return id;
		}

		public List<String> getAttachments() {
			return attachments;
		}

	}

	private Map<String, StagingContextImpl> contexts = new ConcurrentHashMap<>();

	public StagingContextImpl get(String key) {
		return contexts.get(key);
	}

	public StagingContextImpl put(String key, StagingContextImpl value) {
		return contexts.put(key, value);
	}
}
