package step.core.controller;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ControllerSettingAccessorImplTest {

	private ControllerSettingAccessorImpl accessor;
	private InMemoryCollection<ControllerSetting> collection;

	@Before
	public void init() {
		this.collection = new InMemoryCollection<>();
		this.accessor = new ControllerSettingAccessorImpl(collection);
	}

	@Test
	public void testSave() {
		TestHook hook1 = new TestHook();
		TestHook hook2 = new TestHookWithError();
		accessor.addHook("key1", hook1);
		accessor.addHook("key1", hook2);

		// POSITIVE SCENARIO
		ControllerSetting saved = new ControllerSetting("key1", "myValue");
		accessor.save(saved);

		// saved ok - we should find saved element in collection and both hooks have to be called on save
		List<ControllerSetting> found = collection.find(Filters.id(saved.getId()), null, null, null, 0).collect(Collectors.toList());

		Assert.assertEquals(1, found.size());
		Assert.assertTrue(compareSettings(saved, found.get(0)));

		Assert.assertEquals(1, hook1.hookedOnSave.size());
		Assert.assertTrue(compareSettings(hook1.hookedOnSave.get(0), saved));

		Assert.assertEquals(1, hook2.hookedOnSave.size());
		Assert.assertTrue(compareSettings(hook2.hookedOnSave.get(0), saved));

		Assert.assertEquals(0, hook1.hookedOnDelete.size());
		Assert.assertEquals(0, hook2.hookedOnDelete.size());

		hook1.clear();
		hook2.clear();

		// NEGATIVE SCENARIO
		ControllerSetting notSaved = new ControllerSetting("key1", "bad_value");

		try {
			// second hook should throw exception on 'bad_value' - this causes operation rollback and the value should NOT be saved
			// both hooks should be called 'on save' and 'on delete', because the rollback operation performs delete for just saved record
			accessor.save(notSaved);
			Assert.fail("Exception not thrown");
		} catch (ControllerSettingHookRollbackException ex) {
			// ok
		}
		found = collection.find(Filters.id(notSaved.getId()), null, null, null, 0).collect(Collectors.toList());

		Assert.assertEquals(1, hook1.hookedOnSave.size());
		Assert.assertTrue(compareSettings(hook1.hookedOnSave.get(0), notSaved));

		Assert.assertEquals(1, hook2.hookedOnSave.size());
		Assert.assertTrue(compareSettings(hook2.hookedOnSave.get(0), notSaved));

		Assert.assertEquals(1, hook1.hookedOnDelete.size());
		Assert.assertTrue(compareSettings(hook1.hookedOnDelete.get(0), notSaved));
		Assert.assertEquals(1, hook2.hookedOnDelete.size());
		Assert.assertTrue(compareSettings(hook2.hookedOnDelete.get(0), notSaved));

		Assert.assertEquals(0, found.size());

	}

	public boolean compareSettings(ControllerSetting a, ControllerSetting b) {
		return Objects.equals(a.getKey(), b.getKey()) && Objects.equals(a.getValue(), b.getValue());
	}

	private static class TestHook implements ControllerSettingHook {

		protected List<ControllerSetting> hookedOnSave = new ArrayList<>();
		protected List<ControllerSetting> hookedOnDelete = new ArrayList<>();

		@Override
		public void onSettingSave(ControllerSetting setting) {
			hookedOnSave.add(setting);
		}

		@Override
		public void onSettingRemove(ObjectId settingId, ControllerSetting removed) {
			hookedOnDelete.add(removed);
		}

		public void clear(){
			hookedOnSave.clear();
			hookedOnDelete.clear();
		}
	}

	private static class TestHookWithError extends TestHook {
		@Override
		public void onSettingSave(ControllerSetting setting) {
			super.onSettingSave(setting);
			if (setting.getValue().equals("bad_value")) {
				throw new RuntimeException("Test exception");
			}
		}
	}
}