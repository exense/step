package step.commons.buffering;

public interface ObjectFilter<T> {
	
	boolean matches(T o);
}
