package com.engineersbox.quanta.resources.assets.object.serialization;

import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Scene;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ModelDeserializer extends StdDeserializer<Model> {

    private final Scene scene;

    public ModelDeserializer(final Scene scene) {
        this(null, scene);
    }

    public ModelDeserializer(final Class<Model> vc,
                             final Scene scene) {
        super(vc);
        this.scene = scene;
    }

    @Override
    public Model deserialize(final JsonParser jsonParser,
                             final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final JsonNode idNode = node.get("id");
        if (idNode == null) {
            throw new JsonParseException(jsonParser, "Expected id node in model node");
        }
        final String id = idNode.asText();
        final JsonNode pathNode = node.get("path");
        if (pathNode == null) {
            throw new JsonParseException(jsonParser, "Expected pathj node in model node");
        }
        final String path = pathNode.asText();
        final JsonNode isAnimatedNode = node.get("is_animated");
        if (isAnimatedNode == null) {
            throw new JsonParseException(jsonParser, "Expected is_animated node in model node");
        }
        final boolean isAnimated = isAnimatedNode.asBoolean();
        return ModelLoader.loadModel(
                id,
                path,
                this.scene.getTextureCache(),
                this.scene.getMaterialCache(),
                isAnimated
        );
    }

}
