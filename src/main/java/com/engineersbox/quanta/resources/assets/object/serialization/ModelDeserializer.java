package com.engineersbox.quanta.resources.assets.object.serialization;

import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimationData;
import com.engineersbox.quanta.resources.loader.ModelLoader;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.StreamUtils;
import com.engineersbox.quanta.utils.UncheckedThrowsAdapter;
import com.engineersbox.quanta.utils.serialization.SerializationUtils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.engineersbox.quanta.utils.UncheckedThrowsAdapter.unchecked;

public class ModelDeserializer extends StdDeserializer<Model> {

    private final transient Scene scene;

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
        StreamSupport.stream(entitiesNode.spliterator(), false)
                .map(UncheckedThrowsAdapter.<JsonNode, Entity>unchecked(
                        SerializationUtils.OBJECT_MAPPER.reader()
                        .forType(new TypeReference<Entity>(){})::readValue // TODO: Fix this updating previously deserialized objects
                )).map(StreamUtils.passThrough(Entity::updateModelMatrix))
                .map(StreamUtils.passThrough((final Entity entity) -> {
                    if (isAnimated) {
                        entity.setAnimationData(new AnimationData(model.getAnimations().get(0)));
                    }
                })).map(this::copyFixEntity) // BUG: Single short-lived reference used between deserialized objects
                .forEach(entities::add);
        return model;
    }

    /**
     * @implNote
     * The only reason this exists is that for some reason {@link com.fasterxml.jackson.databind.ObjectReader} seems
     * to create weakly referenced objects via {@code ObjectMapper#reader().forType(TypeReference).readValue(JsonNode)}
     * and re-uses the reference between separate invocations for the same type.
     * This causes sequential deserialization of the same object type to end up
     * with identical state, and they are killed by the GC almost immediately
     * after the reader is dropped from the current scope. It's completely broken.
     */
    public Entity copyFixEntity(final Entity entity) {
        final Entity newEntity = new Entity(
                entity.getId(),
                entity.getModelId()
        );
        newEntity.update(entity);
        return newEntity;
    }

}
