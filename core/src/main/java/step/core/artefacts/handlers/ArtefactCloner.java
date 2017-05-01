package step.core.artefacts.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;

public class ArtefactCloner {

	public static <T extends AbstractArtefact> T clone(T in) {
		try {
			
			@SuppressWarnings("unchecked")
			T out = (T) in.getClass().newInstance();
			// force loading of the report node name s 
			//in.getReportNodeName();
			Class<?> clazz = in.getClass();
			do {
				for(Field field:clazz.getDeclaredFields()) {
					if(!Modifier.isStatic(field.getModifiers())) {
						field.setAccessible(true);
						Object object = field.get(in);
						if(!(object instanceof DynamicValue<?>)) {
							if(object instanceof String) {
								String string = (String) object;
								field.set(out, new String(string));
							} else {
								field.set(out, object);
							}							
						} else {
							field.set(out, ((DynamicValue<?>)object).cloneValue());
						}
					}
				}
				clazz = clazz.getSuperclass();
			} while (clazz != Object.class);
			
			return out;
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}
}
