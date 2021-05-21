package step.core.resolvers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Resolver {

	private final List<Function<String, String>> resolvers = new ArrayList<>();

	public void register(Function<String, String> resolver) {
		resolvers.add(resolver);
	}

	public String resolve(String expression) {
		return resolvers.stream().map(r -> r.apply(expression)).filter(Objects::nonNull).findFirst().orElse(expression);
	}

}
