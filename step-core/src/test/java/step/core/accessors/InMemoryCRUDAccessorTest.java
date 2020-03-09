package step.core.accessors;

import java.util.List;

import org.junit.Test;

import junit.framework.Assert;

public class InMemoryCRUDAccessorTest {

	@Test
	public void test() {
		InMemoryCRUDAccessor<AbstractIdentifiableObject> inMemoryCRUDAccessor = new InMemoryCRUDAccessor<>();
		AbstractIdentifiableObject entity = new AbstractIdentifiableObject();
		inMemoryCRUDAccessor.save(entity);
		AbstractIdentifiableObject actualEntity = inMemoryCRUDAccessor.get(entity.getId());
		Assert.assertEquals(entity, actualEntity);
		
		List<AbstractIdentifiableObject> range = inMemoryCRUDAccessor.getRange(0, 1);
		Assert.assertEquals(1, range.size());
		Assert.assertEquals(entity,	range.get(0));
		
		range = inMemoryCRUDAccessor.getRange(10, 1);
		Assert.assertEquals(0, range.size());
	}

}
