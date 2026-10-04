package code.wiosvisual.features.impl.render.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Разбор геометрии Bedrock ("minecraft:geometry"). Оси и знаки повторяют GeckoLib:
 * X зеркалируется, углы X и Y берутся с обратным знаком.
 */
public final class GeoModelParser {
    private GeoModelParser() {
    }

    public static GeoModel parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) {
            return null;
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        int textureWidth = description != null && description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
        int textureHeight = description != null && description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;
        GeoModel model = new GeoModel();
        JsonArray bones = geometry.getAsJsonArray("bones");
        if (bones == null) {
            return model;
        }
        Map<String, GeoModel.Bone> byName = new HashMap<String, GeoModel.Bone>();
        for (JsonElement element : bones) {
            GeoModel.Bone bone = GeoModelParser.parseBone(element.getAsJsonObject(), textureWidth, textureHeight);
            byName.put(bone.name, bone);
        }
        for (JsonElement element : bones) {
            JsonObject object = element.getAsJsonObject();
            GeoModel.Bone bone = byName.get(object.get("name").getAsString());
            GeoModel.Bone parent = object.has("parent") ? byName.get(object.get("parent").getAsString()) : null;
            if (parent != null) {
                parent.childBones.add(bone);
            } else {
                model.topLevelBones.add(bone);
            }
        }
        return model;
    }

    private static GeoModel.Bone parseBone(JsonObject object, int textureWidth, int textureHeight) {
        GeoModel.Bone bone = new GeoModel.Bone(object.get("name").getAsString());
        if (object.has("pivot")) {
            float[] pivot = GeoModelParser.floats(object, "pivot", null);
            bone.pivotX = -pivot[0];
            bone.pivotY = pivot[1];
            bone.pivotZ = pivot[2];
        }
        if (object.has("rotation")) {
            float[] rotation = GeoModelParser.floats(object, "rotation", null);
            bone.rotationX = (float)Math.toRadians(-rotation[0]);
            bone.rotationY = (float)Math.toRadians(-rotation[1]);
            bone.rotationZ = (float)Math.toRadians(rotation[2]);
        }
        if (object.has("cubes")) {
            for (JsonElement element : object.getAsJsonArray("cubes")) {
                bone.childCubes.add(GeoModelParser.parseCube(element.getAsJsonObject(), textureWidth, textureHeight));
            }
        }
        return bone;
    }

    private static GeoModel.Cube parseCube(JsonObject object, int textureWidth, int textureHeight) {
        float[] origin = GeoModelParser.floats(object, "origin", new float[]{0.0f, 0.0f, 0.0f});
        float[] size = GeoModelParser.floats(object, "size", new float[]{1.0f, 1.0f, 1.0f});
        float[] pivot = GeoModelParser.floats(object, "pivot", origin.clone());
        float[] rotation = GeoModelParser.floats(object, "rotation", new float[]{0.0f, 0.0f, 0.0f});
        float inflate = object.has("inflate") ? object.get("inflate").getAsFloat() : 0.0f;
        GeoModel.Cube cube = new GeoModel.Cube(size[0], size[1], size[2]);
        cube.pivotX = -pivot[0];
        cube.pivotY = pivot[1];
        cube.pivotZ = pivot[2];
        cube.rotationX = (float)Math.toRadians(-rotation[0]);
        cube.rotationY = (float)Math.toRadians(-rotation[1]);
        cube.rotationZ = (float)Math.toRadians(rotation[2]);

        float ox = origin[0] - inflate;
        float oy = origin[1] - inflate;
        float oz = origin[2] - inflate;
        float sx = size[0] + inflate * 2.0f;
        float sy = size[1] + inflate * 2.0f;
        float sz = size[2] + inflate * 2.0f;
        float minX = -(ox + sx) / 16.0f;
        float minY = oy / 16.0f;
        float minZ = oz / 16.0f;
        float maxX = -ox / 16.0f;
        float maxY = (oy + sy) / 16.0f;
        float maxZ = (oz + sz) / 16.0f;

        JsonElement uv = object.get("uv");
        if (uv != null && uv.isJsonObject()) {
            // UV задан отдельно для каждой грани.
            JsonObject faces = uv.getAsJsonObject();
            cube.quads[0] = GeoModelParser.perFace(faces, "west", minX, minY, minZ, maxX, maxY, maxZ, -1.0f, 0.0f, 0.0f, textureWidth, textureHeight);
            cube.quads[1] = GeoModelParser.perFace(faces, "east", minX, minY, minZ, maxX, maxY, maxZ, 1.0f, 0.0f, 0.0f, textureWidth, textureHeight);
            cube.quads[2] = GeoModelParser.perFace(faces, "down", minX, minY, minZ, maxX, maxY, maxZ, 0.0f, -1.0f, 0.0f, textureWidth, textureHeight);
            cube.quads[3] = GeoModelParser.perFace(faces, "up", minX, minY, minZ, maxX, maxY, maxZ, 0.0f, 1.0f, 0.0f, textureWidth, textureHeight);
            cube.quads[4] = GeoModelParser.perFace(faces, "north", minX, minY, minZ, maxX, maxY, maxZ, 0.0f, 0.0f, -1.0f, textureWidth, textureHeight);
            cube.quads[5] = GeoModelParser.perFace(faces, "south", minX, minY, minZ, maxX, maxY, maxZ, 0.0f, 0.0f, 1.0f, textureWidth, textureHeight);
        } else {
            // Box UV: развёртка куба начинается с одной точки текстуры.
            float u = 0.0f;
            float v = 0.0f;
            if (uv != null && uv.isJsonArray()) {
                u = uv.getAsJsonArray().get(0).getAsFloat();
                v = uv.getAsJsonArray().get(1).getAsFloat();
            }
            String[] names = new String[]{"west", "east", "down", "up", "north", "south"};
            float[][] normals = new float[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
            for (int i = 0; i < 6; ++i) {
                float[] rect = GeoModelParser.boxUv(names[i], u, v, sx, sy, sz);
                GeoModel.Vertex[] vertices = GeoModelParser.faceVertices(names[i], minX, minY, minZ, maxX, maxY, maxZ,
                        rect[0] / textureWidth, rect[1] / textureHeight, rect[2] / textureWidth, rect[3] / textureHeight);
                cube.quads[i] = new GeoModel.Quad(vertices, normals[i][0], normals[i][1], normals[i][2]);
            }
        }
        return cube;
    }

    private static GeoModel.Quad perFace(JsonObject faces, String face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                         float nx, float ny, float nz, int textureWidth, int textureHeight) {
        if (!faces.has(face)) {
            return null;
        }
        JsonObject data = faces.getAsJsonObject(face);
        float[] uv = GeoModelParser.floats(data, "uv", new float[]{0.0f, 0.0f});
        float[] uvSize = GeoModelParser.floats(data, "uv_size", new float[]{0.0f, 0.0f});
        GeoModel.Vertex[] vertices = GeoModelParser.faceVertices(face, minX, minY, minZ, maxX, maxY, maxZ,
                uv[0] / textureWidth, uv[1] / textureHeight, uvSize[0] / textureWidth, uvSize[1] / textureHeight);
        return new GeoModel.Quad(vertices, nx, ny, nz);
    }

    /** Прямоугольник box UV для грани: {u, v, ширина, высота} в пикселях текстуры. */
    private static float[] boxUv(String face, float u, float v, float sx, float sy, float sz) {
        return switch (face) {
            case "north" -> new float[]{u + sz + sx, v + sz, sx, sy};
            case "south" -> new float[]{u + sz + sx + sz, v + sz, sx, sy};
            case "east" -> new float[]{u, v + sz, sz, sy};
            case "west" -> new float[]{u + sz + sx, v + sz, sz, sy};
            case "up" -> new float[]{u + sz, v, sx, sz};
            case "down" -> new float[]{u + sz + sx, v, sx, sz};
            default -> new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        };
    }

    private static GeoModel.Vertex[] faceVertices(String face, float x1, float y1, float z1, float x2, float y2, float z2,
                                                  float u, float v, float uw, float vh) {
        float u2 = u + uw;
        float v2 = v + vh;
        return switch (face) {
            case "north" -> new GeoModel.Vertex[]{new GeoModel.Vertex(x2, y2, z1, u, v), new GeoModel.Vertex(x1, y2, z1, u2, v), new GeoModel.Vertex(x1, y1, z1, u2, v2), new GeoModel.Vertex(x2, y1, z1, u, v2)};
            case "south" -> new GeoModel.Vertex[]{new GeoModel.Vertex(x1, y2, z2, u, v), new GeoModel.Vertex(x2, y2, z2, u2, v), new GeoModel.Vertex(x2, y1, z2, u2, v2), new GeoModel.Vertex(x1, y1, z2, u, v2)};
            case "east" -> new GeoModel.Vertex[]{new GeoModel.Vertex(x2, y2, z2, u, v), new GeoModel.Vertex(x2, y2, z1, u2, v), new GeoModel.Vertex(x2, y1, z1, u2, v2), new GeoModel.Vertex(x2, y1, z2, u, v2)};
            case "west" -> new GeoModel.Vertex[]{new GeoModel.Vertex(x1, y2, z1, u, v), new GeoModel.Vertex(x1, y2, z2, u2, v), new GeoModel.Vertex(x1, y1, z2, u2, v2), new GeoModel.Vertex(x1, y1, z1, u, v2)};
            case "up" -> new GeoModel.Vertex[]{new GeoModel.Vertex(x1, y2, z1, u, v), new GeoModel.Vertex(x1, y2, z2, u, v2), new GeoModel.Vertex(x2, y2, z2, u2, v2), new GeoModel.Vertex(x2, y2, z1, u2, v)};
            default -> new GeoModel.Vertex[]{new GeoModel.Vertex(x2, y1, z1, u, v), new GeoModel.Vertex(x2, y1, z2, u, v2), new GeoModel.Vertex(x1, y1, z2, u2, v2), new GeoModel.Vertex(x1, y1, z1, u2, v)};
        };
    }

    private static float[] floats(JsonObject object, String key, float[] fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray array = object.getAsJsonArray(key);
        float[] result = new float[array.size()];
        for (int i = 0; i < array.size(); ++i) {
            result[i] = array.get(i).getAsFloat();
        }
        return result;
    }
}
