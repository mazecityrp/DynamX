package fr.dynamx.common.objloader.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jme3.math.Vector3f;
import fr.dynamx.api.dxmodel.DxModelPath;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The data of an external model which has not been supplied yet (see {@link fr.dynamx.common.objloader.ExternalModels}). <br>
 * Built from the metadata file of the model: it knows the name and the bounding box of each object, not the vertices.
 */
public class ExternalObjModelData extends ObjModelData {
    private static final int FORMAT = 1;

    private final List<Mesh> meshes = new ArrayList<>();

    /**
     * @param metadata The content of the metadata file
     * @throws RuntimeException If the metadata are malformed
     */
    public ExternalObjModelData(DxModelPath path, byte[] metadata) {
        super(path, false);
        JsonObject root = new JsonParser().parse(new String(metadata, StandardCharsets.UTF_8)).getAsJsonObject();
        int format = root.get("format").getAsInt();
        if (format != FORMAT) {
            throw new IllegalArgumentException("Unsupported metadata format " + format);
        }
        for (JsonElement element : root.get("meshes").getAsJsonArray()) {
            JsonObject mesh = element.getAsJsonObject();
            meshes.add(new Mesh(mesh.get("name").getAsString(), readVector(mesh.get("min")), readVector(mesh.get("max"))));
        }
    }

    private static float[] readVector(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) {
            throw new IllegalArgumentException("A vector should have 3 components");
        }
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException("The vertices of " + getObjModelPath() + " are not available: it is an external model which has not been supplied yet. " +
                "If it uses complex collisions, put its .dc file in the pack");
    }

    /**
     * @return The first object having this name, ignoring the case, as the real model would do
     */
    @Nullable
    private Mesh getMesh(String name) {
        for (Mesh mesh : meshes) {
            if (mesh.name.equalsIgnoreCase(name)) {
                return mesh;
            }
        }
        return null;
    }

    private static Vector3f set(@Nullable Vector3f result, float x, float y, float z) {
        return result == null ? new Vector3f(x, y, z) : result.set(x, y, z);
    }

    @Override
    public float[] getVerticesPos(String objectName) {
        throw unavailable();
    }

    @Override
    public float[] getVerticesPos() {
        throw unavailable();
    }

    @Override
    public int[] getMeshIndices(String objectName) {
        throw unavailable();
    }

    @Override
    public int[] getAllMeshIndices() {
        throw unavailable();
    }

    @Override
    public Vector3f getMeshMin(String name, @Nullable Vector3f result) {
        Mesh mesh = getMesh(name);
        return mesh == null ? null : set(result, mesh.min[0], mesh.min[1], mesh.min[2]);
    }

    @Override
    public Vector3f getMeshMax(String name, @Nullable Vector3f result) {
        Mesh mesh = getMesh(name);
        return mesh == null ? null : set(result, mesh.max[0], mesh.max[1], mesh.max[2]);
    }

    @Override
    public Vector3f getMinOfModel(@Nullable Vector3f result) {
        if (meshes.isEmpty()) {
            return set(result, 0, 0, 0);
        }
        float[] min = meshes.get(0).min.clone();
        for (Mesh mesh : meshes) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], mesh.min[i]);
            }
        }
        return set(result, min[0], min[1], min[2]);
    }

    @Override
    public Vector3f getMaxOfModel(@Nullable Vector3f result) {
        if (meshes.isEmpty()) {
            return set(result, 0, 0, 0);
        }
        float[] max = meshes.get(0).max.clone();
        for (Mesh mesh : meshes) {
            for (int i = 0; i < 3; i++) {
                max[i] = Math.max(max[i], mesh.max[i]);
            }
        }
        return set(result, max[0], max[1], max[2]);
    }

    /**
     * @return Always null: the objects themselves are not available
     */
    @Override
    public ObjObjectData getObjObject(String objectName) {
        return null;
    }

    @Override
    public List<String> getMeshNames() {
        List<String> names = new ArrayList<>(meshes.size());
        for (Mesh mesh : meshes) {
            names.add(mesh.name.toLowerCase());
        }
        return names;
    }

    private static class Mesh {
        private final String name;
        private final float[] min;
        private final float[] max;

        private Mesh(String name, float[] min, float[] max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }
    }
}
