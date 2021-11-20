package step.controller.swagger;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.bson.types.ObjectId;

import java.util.Iterator;

public class ObjectIdAwareConverter implements ModelConverter {

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType _type = Json.mapper().constructType(type.getType());

        if (_type != null) {
            Class<?> cls = _type.getRawClass();
            if (ObjectId.class.isAssignableFrom(cls)) {
                return new StringSchema().pattern("[a-f0-9]{24}}");
            }
        }
        if (chain.hasNext()) {
            return chain.next().resolve(type, context, chain);
        } else {
            return null;
        }
    }
}