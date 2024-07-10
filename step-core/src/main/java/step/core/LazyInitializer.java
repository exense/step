package step.core;


public abstract class LazyInitializer<T>  {

    private static final Object NO_INIT = new Object();
    private volatile T object;

    public LazyInitializer() {
        this.object = (T) NO_INIT;
    }

    public T get()  {
        T result = this.object;
        if (result == NO_INIT) {
            synchronized(this) {
                result = this.object;
                if (result == NO_INIT) {
                    this.object = result = this.initialize();
                }
            }
        }

        return result;
    }

    protected abstract T initialize() ;

    public boolean isInitialized() {
        return this.object != NO_INIT;
    }
}
