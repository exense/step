package step.core.controller;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;

import java.util.*;
import java.util.stream.Collectors;

public class ControllerSettingAccessorImplTest {

	private static final Logger log = LoggerFactory.getLogger(ControllerSettingAccessorImplTest.class);

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
		List<ControllerSetting> found = findByKey("key1");

		compareSettings(Arrays.asList(saved), found);
		compareSettings(Arrays.asList(saved), hook1.hookedOnSave);
		compareSettings(Arrays.asList(saved), hook2.hookedOnSave);

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

		// found collection contains saved value from positive scenario
		found = findByKey(notSaved.getKey());
		compareSettings(Arrays.asList(saved), found);

		compareSettings(Arrays.asList(notSaved), hook1.hookedOnSave);
		compareSettings(Arrays.asList(notSaved), hook2.hookedOnSave);

		compareSettings(Arrays.asList(notSaved), hook1.hookedOnDelete);
		compareSettings(Arrays.asList(notSaved), hook2.hookedOnDelete);
	}

	private List<ControllerSetting> findByKey(String key) {
		List<ControllerSetting> found = collection.find(Filters.equals("key", key), null, null, null, 0).collect(Collectors.toList());
		String print = print(found);
		log.info("Found: {}", print);
		return found;
	}

	private String print(List<ControllerSetting> found) {
		String print = "[";
		StringJoiner joiner = new StringJoiner(",");
		found.forEach(setting -> joiner.add(print(setting)));
		print += joiner.toString();
		print += "]";
		return print;
	}

	private String print(ControllerSetting setting) {
		return "{" + setting.getKey() + " -> " + setting.getValue() + "}";
	}

	@Test
	public void testRemove() {

		// POSITIVE SCENARIO
		ControllerSetting saved1 = new ControllerSetting("key1", "bad_value");
		ControllerSetting saved2 = new ControllerSetting("key1", "myValue");
		accessor.save(saved1);
		accessor.save(saved2);

		TestHook hook1 = new TestHook();
		TestHook hook2 = new TestHookWithError();
		accessor.addHook("key1", hook1);
		accessor.addHook("key1", hook2);

		accessor.remove(saved2.getId());

		List<ControllerSetting> found = findByKey("key1");
		compareSettings(Arrays.asList(saved1), found);

		compareSettings(Arrays.asList(saved2), hook1.hookedOnDelete);
		compareSettings(Arrays.asList(saved2), hook2.hookedOnDelete);

		Assert.assertEquals(0, hook1.hookedOnSave.size());
		Assert.assertEquals(0, hook2.hookedOnSave.size());

		hook1.clear();
		hook2.clear();

		// NEGATIVE SCENARIO
		try {
			// second hook should throw exception on 'bad_value' - this causes operation rollback and the value should NOT be deleted
			// both hooks should be called 'on save' and 'on delete', because the rollback operation performs compensating save for just deleted record
			accessor.remove(saved1.getId());
		}  catch (ControllerSettingHookRollbackException ex) {
			// ok
		}

		// value should not be removed
		found = findByKey("key1");
		compareSettings(Arrays.asList(saved1), found);

		// both hooks have to be called for 'on delete' and for compensating 'on save'
		compareSettings(Arrays.asList(saved1), hook1.hookedOnDelete);
		compareSettings(Arrays.asList(saved1), hook2.hookedOnDelete);

		compareSettings(Arrays.asList(saved1), hook1.hookedOnSave);
		compareSettings(Arrays.asList(saved1), hook2.hookedOnSave);
	}

	@Test
	public void testSaveCollection() {
		TestHook hook1 = new TestHook();
		TestHook hook2 = new TestHookWithError();
		accessor.addHook("key1", hook1);
		accessor.addHook("key1", hook2);

		// POSITIVE SCENARIO
		ControllerSetting saved1 = new ControllerSetting("key1", "myValue");
		ControllerSetting saved2 = new ControllerSetting("key1", "myValue2");
		accessor.save(Arrays.asList(saved1, saved2));

		// saved ok - we should find saved element in collection and both hooks have to be called on save
		List<ControllerSetting> found = findByKey(saved1.getKey());

		compareSettings(Arrays.asList(saved1, saved2), found);
		compareSettings(Arrays.asList(saved1, saved2), hook1.hookedOnSave);
		compareSettings(Arrays.asList(saved1, saved2), hook2.hookedOnSave);

		Assert.assertEquals(0, hook1.hookedOnDelete.size());
		Assert.assertEquals(0, hook2.hookedOnDelete.size());

		hook1.clear();
		hook2.clear();

		// NEGATIVE SCENARIO
		ControllerSetting notSaved1 = new ControllerSetting("key1", "myValue3");
		ControllerSetting notSaved2 = new ControllerSetting("key1", "bad_value");

		try {
			// second hook should throw exception on 'bad_value' - this causes operation rollback and the value should NOT be saved
			// both hooks should be called 'on save' and 'on delete', because the rollback operation performs delete for just saved record
			accessor.save(Arrays.asList(notSaved1, notSaved2));
			Assert.fail("Exception not thrown");
		} catch (ControllerSettingHookRollbackException ex) {
			// ok
		}

		// found collection contains saved value from positive scenario
		found = findByKey("key1");
		compareSettings(Arrays.asList(saved1, saved2), found);

		compareSettings(Arrays.asList(notSaved1, notSaved2), hook1.hookedOnSave);
		compareSettings(Arrays.asList(notSaved1, notSaved2), hook2.hookedOnSave);

		compareSettings(Arrays.asList(notSaved1, notSaved2), hook1.hookedOnDelete);
		compareSettings(Arrays.asList(notSaved1, notSaved2), hook2.hookedOnDelete);
	}

	public void compareSettings(List<ControllerSetting> a, List<ControllerSetting> b){
		Assert.assertEquals(a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			Assert.assertTrue(compareSettings(a.get(i), b.get(i)));
		}
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

		@Override
		public void onSettingRemove(ObjectId settingId, ControllerSetting removed) {
			super.onSettingRemove(settingId, removed);
			if (removed.getValue().equals("bad_value")) {
				throw new RuntimeException("Test exception");
			}
		}
	}
}