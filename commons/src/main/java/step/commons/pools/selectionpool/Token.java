package step.commons.pools.selectionpool;


public class Token<T extends Identity> {

	final T object;
	
	volatile boolean available;
	
	volatile boolean invalidated;
	
	volatile long lastTouch;
	
	public Token(T object) {
		this.object = object;
	}

	public T getObject() {
		return object;
	}
	
	public boolean isFree() {
		return available && !invalidated;
	}
}
