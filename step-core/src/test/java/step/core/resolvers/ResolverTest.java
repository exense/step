package step.core.resolvers;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResolverTest {

	@Test
	public void test() {
		final Resolver resolver = new Resolver();
		String resolve = resolver.resolve("test");
		assertEquals("test", resolve);
		resolve = resolver.resolve(null);
		assertEquals(null, resolve);
		resolver.register(s -> "resolvedValue");
		resolve = resolver.resolve("test");
		assertEquals("resolvedValue", resolve);
		resolve = resolver.resolve(null);
		assertEquals("resolvedValue", resolve);
	}

}
