package com.engineersbox.quanta.scene.serialization;

import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.rendering.view.Projection;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.serialization.ModelDeserializer;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.SkyBox;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.SceneLights;
import com.engineersbox.quanta.utils.serialization.SerializationUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.engineersbox.quanta.utils.UncheckedThrowsAdapter.uncheckedFunction;

public class SceneDeserializer extends StdDeserializer<Scene> {

    private static final Logger LOGGER = LogManager.getLogger(SceneDeserializer.class);

    private final int width;
    private final int height;

    public SceneDeserializer() {
        this(0, 0);
    }

    public SceneDeserializer(final int width,
                             final int height) {
        this(
                null,
                width,
                height
        );
    }

    public SceneDeserializer(final Class<Scene> vc,
                             final int width,
                             final int height) {
        super(vc);
        this.width = width;
        this.height = height;
    }

    @Override
    public Scene deserialize(final JsonParser jsonParser,
                             final DeserializationContext deserializationContext) throws IOException {
        final Scene scene = new Scene(this.width, this.height);
        final ObjectCodec codec = jsonParser.getCodec();
        final JsonNode node = codec.readTree(jsonParser);

        final JsonNode modelMapNode = node.get("models");
        if (modelMapNode == null) {
            throw new JsonParseException(jsonParser, "Expected model map node");
        }
        deserializeModelMap(
                modelMapNode.fields(),
                scene,
                codec,
                deserializationContext
        );

        final JsonNode projectionNode = node.get("projection");
        if (projectionNode == null) {
            throw new JsonParseException(jsonParser, "Expected projection node");
        }
        final Projection projection = SerializationUtils.OBJECT_MAPPER.reader()
                .forType(new TypeReference<Projection>(){})
                .readValue(projectionNode.traverse(codec));
        scene.getProjection().update(projection);

        final JsonNode cameraNode = node.get("camera");
        if (cameraNode == null) {
            throw new JsonParseException(jsonParser, "Expected camera node");
        }
        final Camera camera = SerializationUtils.OBJECT_MAPPER.reader()
                .forType(new TypeReference<Camera>(){})
                .readValue(cameraNode.traverse(codec));
        scene.getCamera().update(camera);

        final JsonNode sceneLightsNode = node.get("scene_lights");
        if (sceneLightsNode != null) {
            final SceneLights sceneLights = SerializationUtils.OBJECT_MAPPER.reader()
                    .forType(new TypeReference<SceneLights>(){})
                    .readValue(sceneLightsNode.traverse(codec));
            scene.setSceneLights(sceneLights);
        }

        final JsonNode skyBoxNode = node.get("skybox");
        if (skyBoxNode != null) {
            final SkyBoxDeserializer skyBoxDeserializer = new SkyBoxDeserializer(scene);
            final SkyBox skyBox = skyBoxDeserializer.deserialize(
                    skyBoxNode.traverse(codec),
                    deserializationContext
            );
            scene.setSkyBox(skyBox);
        }

        final JsonNode fogNode = node.get("fog");
        if (fogNode != null) {
            final Fog fog = SerializationUtils.OBJECT_MAPPER.reader()
                    .forType(new TypeReference<Fog>(){})
                    .readValue(fogNode.traverse(codec));
            scene.setFog(fog);
        }
        return scene;
    }

    private void deserializeModelMap(final Iterator<Map.Entry<String, JsonNode>> modelMapFields,
                                     final Scene scene,
                                     final ObjectCodec codec,
                                     final DeserializationContext deserializationContext) {
        final Map<String, Model> modelsMap = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(
                                modelMapFields,
                                0
                        ),
                        false
                ).map(uncheckedFunction((final Map.Entry<String, JsonNode> entry) -> {
                    final JsonDeserializer<? extends Model> deserializer = parseDeserializer(
                            entry.getValue(),
                            scene
                    );
                    return ImmutablePair.of(
                            entry.getKey(),
                            deserializer.deserialize(
                                    entry.getValue().traverse(codec),
                                    deserializationContext
                            )
                    );
                })).collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue
                ));
        scene.getModels().putAll(modelsMap);
    }

    @SuppressWarnings({"unchecked"})
    private JsonDeserializer<? extends Model> parseDeserializer(final JsonNode node,
                                                                final Scene scene) throws ClassNotFoundException {
        final JsonNode typeNode = node.get("type");
        if (typeNode == null) {
            throw new IllegalStateException("Expected type field in model");
        }
        final Class<?> modelClass = Class.forName(typeNode.asText());
        if (!modelClass.isAnnotationPresent(JsonDeserialize.class)) {
            SceneDeserializer.LOGGER.debug("Found no @JsonDeserialize annotation on model class {}, defaulting to ModelDeserializer", modelClass.getName());
            return new ModelDeserializer(scene);
        }
        final JsonDeserialize annotation = modelClass.getAnnotation(JsonDeserialize.class);
        final Class<? extends JsonDeserializer<?>> deserializer = (Class<? extends JsonDeserializer<?>>) annotation.using();
        if (deserializer.equals(JsonDeserializer.None.class)) {
            SceneDeserializer.LOGGER.debug("Model class {} @JsonDeserialize annotation specified none for using(), defaulting to ModelDeserializer", modelClass.getName());
            return new ModelDeserializer(scene);
        }
        try {
            final Constructor<?> constructor = deserializer.getConstructor(Scene.class);
            final Type typeVariable = ((ParameterizedType) deserializer.getGenericSuperclass()).getActualTypeArguments()[0];
            final Class<?> typeVarClass = Class.forName(typeVariable.toString());
            if (!typeVarClass.isAssignableFrom(Model.class)) {
                throw new IllegalStateException(String.format(
                        "Expected deserializer %s to build an instance of Model not %s",
                        deserializer.getName(),
                        typeVarClass.getName()
                ));
            }
            return (JsonDeserializer<? extends Model>) constructor.newInstance(scene);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Expected deserializer to take single Scene object in constructor");
        } catch (final InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Unable to instantiate custom deserializer for type %s",
                    deserializer.getName()
            ), e);
        }
    }

}
