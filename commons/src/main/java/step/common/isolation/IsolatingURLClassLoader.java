package step.common.isolation;

import java.net.URL;
import java.net.URLClassLoader;

public class IsolatingURLClassLoader extends URLClassLoader {

	public IsolatingURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}
	
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
    		// avoid class loader delegation for classes that are not part of the grid project 
    		// and force this classloader instance to (re)load them.
    		// classes that are part of the grid project (step.grid.*) are not reloaded to enable 
    		// communication between parent message handlers
    		if((!name.startsWith("step.grid"))) {
    			synchronized (getClassLoadingLock(name)) {
                    // First, check if the class has already been loaded
    				Class<?> c = findLoadedClass(name);
    				
    				if(c==null) {
    					try {
    						c = findClass(name);
    					} catch (ClassNotFoundException e2) {
    						c = super.findSystemClass(name);
    						
    					}
    					
    				}

    			
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
    		} else {
    			return super.loadClass(name, resolve);
    		}
        }

}
