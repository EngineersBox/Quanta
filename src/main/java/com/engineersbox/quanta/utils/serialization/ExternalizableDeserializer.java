package com.engineersbox.quanta.utils.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;

public class ExternalizableDeserializer<T extends Externalizable> extends StdDeserializer<T> implements ContextualDeserializer {

    private static final String TARGET_CLASS_ATTRIBUTE_KEY = "target_class";

    private final T instance;

    public ExternalizableDeserializer() {
        this(null);
    }

    public ExternalizableDeserializer(final T instance) {
        this(null, instance);
    }

    public ExternalizableDeserializer(final Class<T> vc,
                                      final T instance) {
        super(vc);
        this.instance = instance;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T deserialize(final JsonParser jsonParser,
                         final DeserializationContext context) throws IOException, JacksonException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final byte[] data = node.binaryValue();
        try (final ObjectInput objectInput = new ObjectInputStream(new ByteArrayInputStream(data))) {
            this.instance.readExternal(objectInput);
            return this.instance;
        } catch (final ClassNotFoundException e) {
            throw new JsonParseException(jsonParser, "Cannot find associated class of target instance", e);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext context, final BeanProperty beanProperty) throws JsonMappingException {
        T target = this.instance;
        if (target == null) {
            try {
                final Class<T> targetClass = (Class<T>) beanProperty.getType().getRawClass();
                target = ConstructorUtils.invokeConstructor(targetClass);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException("Cannot find default constructor for externalizable", e);
            }
        }
        return new ExternalizableDeserializer<>(target);
    }
}
