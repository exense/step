package step.core.accessors;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class CachedCRUDAccessorTest extends InMemoryCRUDAccessorTest {

	private InMemoryCRUDAccessor<AbstractIdentifiableObject> underlyingAccessor = new InMemoryCRUDAccessor<>();
	
	@Before
	public void before() {
		accessor = new CachedCRUDAccessor<AbstractIdentifiableObject>(underlyingAccessor);
	}
	
	@Test
	public void testCaching() {
		AbstractIdentifiableObject entity = new AbstractOrganizableObject();
		underlyingAccessor.save(entity);
		
		((CachedCRUDAccessor<?>)accessor).reloadCache();
		
		AbstractIdentifiableObject actual = accessor.get(entity.getId());
		assertEquals(entity, actual);
		
		AbstractIdentifiableObject entity2 = new AbstractIdentifiableObject();
		accessor.save(entity2);
		
		// Ensure it has been saved to the underlying accessor
		actual = underlyingAccessor.get(entity2.getId());
		assertEquals(entity2, actual);
		
		// Ensure the cache has been updated
		actual = accessor.get(entity2.getId());
		assertEquals(entity2, actual);
		
		accessor.remove(entity2.getId());
		
		// Ensure it has been saved to the underlying accessor
		actual = underlyingAccessor.get(entity2.getId());
		assertNull(actual);
		
		// Ensure the cache has been updated
		actual = accessor.get(entity2.getId());
		assertNull(actual);
	}

}
