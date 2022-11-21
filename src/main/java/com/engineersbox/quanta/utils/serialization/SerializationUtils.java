package com.engineersbox.quanta.utils.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationUtils {

    private SerializationUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

}
