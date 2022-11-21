package com.engineersbox.quanta.utils.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.*;

public class ExternalizableSerializer<T extends Externalizable> extends StdSerializer<T> {

    public ExternalizableSerializer() {
        this(null);
    }

    public ExternalizableSerializer(final Class<T> vc) {
        super(vc);
    }

    @Override
    public void serialize(final T externalizable,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(0);
             final ObjectOutput output = new ObjectOutputStream(baos)) {
            externalizable.writeExternal(output);
            output.flush();
            jsonGenerator.writeBinary(baos.toByteArray());
        }
    }
}
