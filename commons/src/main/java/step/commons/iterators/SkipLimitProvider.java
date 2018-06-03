package step.commons.iterators;

import java.util.List;

public interface SkipLimitProvider<T> {

	public abstract List<T> getBatch(int skip, int limit);

}
