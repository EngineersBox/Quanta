package com.engineersbox.quanta.scene.serialization;

import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.SkyBox;
import com.engineersbox.quanta.utils.serialization.SerializationUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class SkyBoxDeserializer extends StdDeserializer<SkyBox> {

    private final Scene scene;

    public SkyBoxDeserializer(final Scene scene) {
        this(null, scene);
    }

    public SkyBoxDeserializer(final Class<SkyBox> vc,
                              final Scene scene) {
        super(vc);
        this.scene = scene;
    }

    @Override
    public SkyBox deserialize(final JsonParser jsonParser,
                              final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final JsonNode modelNode = node.get("model");
        if (modelNode == null) {
            throw new JsonParseException(jsonParser, "Expected model node in SkyBox");
        }
        final JsonNode modelPath = modelNode.get("path");
        if (modelPath == null) {
            throw new JsonParseException(jsonParser, "Expected model path in model node");
        }
        final SkyBox skyBox = new SkyBox(
                modelPath.asText(),
                this.scene.getTextureCache(),
                this.scene.getMaterialCache()
        );
        final JsonNode entityNode = node.get("entity");
        if (entityNode == null) {
            throw new JsonParseException(jsonParser, "Expected entity node in SkyBox");
        }
        final Entity entity = SerializationUtils.OBJECT_MAPPER.reader()
                .forType(new TypeReference<Entity>(){})
                .readValue(entityNode);
        skyBox.getEntity().update(entity);
        return skyBox;
    }
}
