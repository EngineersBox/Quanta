package com.engineersbox.quanta.utils.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

public class ExternalizableDeserializer<T extends Externalizable> extends StdDeserializer<T> implements ContextualDeserializer {

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

    @Override
    public T deserialize(final JsonParser jsonParser,
                         final DeserializationContext context) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final byte[] data = node.binaryValue();
        try (final ObjectInput objectInput = new ObjectInputStream(new ByteArrayInputStream(data))) {
            this.instance.readExternal(objectInput);
            return this.instance;
        } catch (final ClassNotFoundException e) {
            throw new JsonParseException(jsonParser, "Cannot find associated class of target instance", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext context, final BeanProperty beanProperty) {
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
