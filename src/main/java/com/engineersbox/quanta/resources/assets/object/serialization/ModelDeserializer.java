package com.engineersbox.quanta.resources.assets.object.serialization;

import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.serialization.SerializationUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

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
                             final DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final JsonNode idNode = node.get("id");
        if (idNode == null) {
            throw new JsonParseException(jsonParser, "Expected id node in model node");
        }
        final String id = idNode.asText();
        final JsonNode pathNode = node.get("path");
        if (pathNode == null) {
            throw new JsonParseException(jsonParser, "Expected path node in model node");
        }
        final String path = pathNode.asText();
        final JsonNode isAnimatedNode = node.get("is_animated");
        if (isAnimatedNode == null) {
            throw new JsonParseException(jsonParser, "Expected is_animated node in model node");
        }
        final boolean isAnimated = isAnimatedNode.asBoolean();
        final Model model = ModelLoader.loadModel(
                id,
                path,
                this.scene.getTextureCache(),
                this.scene.getMaterialCache(),
                isAnimated
        );
        final JsonNode entitiesNode = node.get("entities");
        if (entitiesNode == null || !entitiesNode.isArray()) {
            throw new JsonParseException(jsonParser, "Expected entities node in model node");
        }
        final List<Entity> entities = model.getEntities();
        for (final JsonNode entityNode : entitiesNode) {
            final Entity entity = SerializationUtils.OBJECT_MAPPER.reader()
                    .forType(new TypeReference<Entity>() {})
                    .readValue(entityNode);
            entity.updateModelMatrix();
            entities.add(entity);
        }
        return model;
    }

}
