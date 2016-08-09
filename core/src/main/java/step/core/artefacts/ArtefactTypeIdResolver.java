package step.core.artefacts;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ArtefactTypeIdResolver implements TypeIdResolver {

	@Override
	public Id getMechanism() {
		return Id.CUSTOM;
	}

	@Override
	public String idFromBaseType() {
		return null;
	}

	@Override
	public String idFromValue(Object arg0) {
		return idFromClass(arg0.getClass());
	}
	
	@SuppressWarnings("unchecked")
	private String idFromClass(Class<?>c) {
		return ArtefactRegistry.getArtefactName((Class<? extends AbstractArtefact>) c);
	}

	@Override
	public String idFromValueAndType(Object arg0, Class<?> arg1) {
		return idFromClass(arg1);
	}

	@Override
	public void init(JavaType arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public JavaType typeFromId(String arg0) {
		return TypeFactory.defaultInstance().uncheckedSimpleType(ArtefactRegistry.getInstance().getArtefactType(arg0));
	}

	@Override
	public JavaType typeFromId(DatabindContext arg0, String arg1) {
		return typeFromId(arg1);
	}

}
