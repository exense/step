package step.core.controller;

import org.bson.types.ObjectId;
import step.core.collections.inmemory.InMemoryCollection;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryControllerSettingAccessor extends ControllerSettingAccessorImpl implements ControllerSettingAccessor {

	public InMemoryControllerSettingAccessor() {
		super(new InMemoryCollection<ControllerSetting>());
	}

	protected ControllerSetting copy(ControllerSetting controllerSetting){
		if(controllerSetting != null) {
			ControllerSetting copy = new ControllerSetting(controllerSetting.getKey(), controllerSetting.getValue());
			copy.setId(controllerSetting.getId());
			return copy;
		}
		return null;
	}

	// in all methods below we need to return a clone, because otherwise a caller can get and modify the in-memory object via simple setters


	@Override
	public ControllerSetting getSettingByKey(String key) {
		return copy(super.getSettingByKey(key));
	}

	@Override
	public ControllerSetting get(ObjectId id) {
		return copy(super.get(id));
	}

	@Override
	public ControllerSetting findByCriteria(Map<String, String> criteria) {
		return copy(super.findByCriteria(criteria));
	}

	@Override
	public Stream<ControllerSetting> findManyByCriteria(Map<String, String> criteria) {
		return super.findManyByCriteria(criteria).map(this::copy);
	}

	@Override
	public ControllerSetting findByAttributes(Map<String, String> attributes) {
		return copy(super.findByAttributes(attributes));
	}

	@Override
	public Spliterator<ControllerSetting> findManyByAttributes(Map<String, String> attributes) {
		// TODO: copy value in spliterator
		return super.findManyByAttributes(attributes);
	}

	@Override
	public ControllerSetting findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return copy(super.findByAttributes(attributes, attributesMapKey));
	}

	@Override
	public Spliterator<ControllerSetting> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		// TODO: copy value in spliterator
		return super.findManyByAttributes(attributes, attributesMapKey);
	}

	@Override
	public Iterator<ControllerSetting> getAll() {
		return this.stream().map(this::copy).iterator();
	}

	@Override
	public Stream<ControllerSetting> stream() {
		return super.stream().map(this::copy);
	}

	@Override
	public List<ControllerSetting> getRange(int skip, int limit) {
		return super.getRange(skip, limit).stream().map(this::copy).collect(Collectors.toList());
	}

}
