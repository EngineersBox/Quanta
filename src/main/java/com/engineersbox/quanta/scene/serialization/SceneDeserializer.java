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
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SceneDeserializer extends StdDeserializer<Scene> {

    private final int width;
    private final int height;

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
                             final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final Scene scene = new Scene(this.width, this.height);
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final JsonNode modelMapNode = node.get("models");
        if (modelMapNode == null) {
            throw new JsonParseException(jsonParser, "Expected model map node");
        }
        final Iterator<Map.Entry<String, JsonNode>> modelMapFields = modelMapNode.fields();
        final ModelDeserializer modelDeserializer = new ModelDeserializer(scene);
        final Map<String, Model> modelsMap = StreamSupport.stream(Spliterators.spliteratorUnknownSize(modelMapFields, 0), false)
                .map((final Map.Entry<String, JsonNode> entry) -> {
                    try {
                        return ImmutablePair.of(
                                entry.getKey(),
                                modelDeserializer.deserialize(entry.getValue().traverse(), deserializationContext)
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue
                ));
        scene.getModels().putAll(modelsMap);

        final JsonNode projectionNode = node.get("projection");
        if (projectionNode == null) {
            throw new JsonParseException(jsonParser, "Expected projection node");
        }
        final Projection projection = SerializationUtils.OBJECT_MAPPER.reader()
                .forType(new TypeReference<Projection>(){})
                .readValue(projectionNode.traverse());
        scene.getProjection().update(projection);

        final JsonNode cameraNode = node.get("camera");
        if (cameraNode == null) {
            throw new JsonParseException(jsonParser, "Expected camera node");
        }
        final Camera camera = SerializationUtils.OBJECT_MAPPER.reader()
                .forType(new TypeReference<Camera>(){})
                .readValue(cameraNode.traverse());
        scene.getCamera().update(camera);

        final JsonNode sceneLightsNode = node.get("scene_lights");
        if (sceneLightsNode != null) {
            final SceneLights sceneLights = SerializationUtils.OBJECT_MAPPER.reader()
                    .forType(new TypeReference<SceneLights>(){})
                    .readValue(sceneLightsNode.traverse());
            scene.setSceneLights(sceneLights);
        }

        final JsonNode skyBoxNode = node.get("skybox");
        if (skyBoxNode != null) {
            final SkyBoxDeserializer skyBoxDeserializer = new SkyBoxDeserializer(scene);
            final SkyBox skyBox = skyBoxDeserializer.deserialize(
                    skyBoxNode.traverse(),
                    deserializationContext
            );
            scene.setSkyBox(skyBox);
        }

        final JsonNode fogNode = node.get("fog");
        if (fogNode != null) {
            final Fog fog = SerializationUtils.OBJECT_MAPPER.reader()
                    .forType(new TypeReference<Fog>(){})
                    .readValue(fogNode.traverse());
            scene.setFog(fog);
        }
        return scene;
    }
}
