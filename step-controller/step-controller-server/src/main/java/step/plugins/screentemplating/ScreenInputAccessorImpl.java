package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonObjectBuilder;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.core.json.JsonProviderCache;

public class ScreenInputAccessorImpl extends AbstractCRUDAccessor<ScreenInput> implements ScreenInputAccessor {

	public ScreenInputAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "screenInputs", ScreenInput.class);
	}

	@Override
	public List<ScreenInput> getScreenInputsByScreenId(String screenId) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		builder.add("screenId", screenId);

		List<ScreenInput> result = new ArrayList<>();
		String query = builder.build().toString();
		collection.find(query).as(ScreenInput.class).forEach(r->result.add(r));
		
		return result.stream().sorted(Comparator.comparingInt(ScreenInput::getPosition)
				.thenComparing(Comparator.comparing(ScreenInput::getId))).collect(Collectors.toList());
	}

}
