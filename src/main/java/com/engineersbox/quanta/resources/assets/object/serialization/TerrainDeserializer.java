package com.engineersbox.quanta.resources.assets.object.serialization;

import com.engineersbox.quanta.resources.assets.object.builtin.Terrain;
import com.engineersbox.quanta.scene.Scene;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class TerrainDeserializer extends StdDeserializer<Terrain> {

    private final transient Scene scene;

    public TerrainDeserializer(final Scene scene) {
        this(null, scene);
    }

    public TerrainDeserializer(final Class<Terrain> vc,
                               final Scene scene) {
        super(vc);
        this.scene = scene;
    }

    @Override
    public Terrain deserialize(final JsonParser jsonParser,
                               final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final JsonNode idNode = node.get("id");
        if (idNode == null) {
            throw new JsonParseException(jsonParser, "Expected id node in terrain node");
        }
        final JsonNode isInternalNode = node.get("is_internal");
        if (isInternalNode == null) {
            throw new JsonParseException(jsonParser, "Expected is_internal node in terrain node");
        }
        final JsonNode sizeNode = node.get("size");
        if (sizeNode == null) {
            throw new JsonParseException(jsonParser, "Expected size node in terrain node");
        }
        final JsonNode scaleNode = node.get("scale");
        if (scaleNode == null) {
            throw new JsonParseException(jsonParser, "Expected scale node in terrain node");
        }
        final JsonNode minYNode = node.get("min_y");
        if (minYNode == null) {
            throw new JsonParseException(jsonParser, "Expected min_y node in terrain node");
        }
        final JsonNode maxYNode = node.get("max_y");
        if (maxYNode == null) {
            throw new JsonParseException(jsonParser, "Expected max_y node in terrain node");
        }
        final JsonNode textureIncrementNode = node.get("texture_increment");
        if (textureIncrementNode == null) {
            throw new JsonParseException(jsonParser, "Expected texture_increment node in terrain node");
        }
        final JsonNode heightMapFileNode = node.get("height_map_file");
        if (heightMapFileNode == null) {
            throw new JsonParseException(jsonParser, "Expected height_map_file node in terrain node");
        }
        final JsonNode textureFileNode = node.get("texture_file");
        if (textureFileNode == null) {
            throw new JsonParseException(jsonParser, "Expected texture_file node in terrain node");
        }
        return new Terrain(
                idNode.asText(),
                sizeNode.asInt(),
                (float) scaleNode.asDouble(),
                (float) minYNode.asDouble(),
                (float) maxYNode.asDouble(),
                textureIncrementNode.asInt(),
                heightMapFileNode.asText(),
                textureFileNode.asText(),
                this.scene.getMaterialCache(),
                this.scene.getTextureCache(),
                isInternalNode.asBoolean()
        );
    }
}
