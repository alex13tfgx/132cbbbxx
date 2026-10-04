package code.wiosvisual.features.impl.render.cosmetics;

import java.util.ArrayList;
import java.util.List;

/**
 * Геометрия модели в формате Bedrock/GeckoLib: дерево костей, у костей — кубы,
 * у кубов — по шесть граней с готовыми вершинами и UV. Координаты вершин уже в блоках.
 */
public final class GeoModel {
    public final List<Bone> topLevelBones = new ArrayList<Bone>();

    public Bone findBone(String name) {
        for (Bone bone : this.topLevelBones) {
            Bone found = bone.find(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static final class Bone {
        public final String name;
        public final List<Bone> childBones = new ArrayList<Bone>();
        public final List<Cube> childCubes = new ArrayList<Cube>();
        /** Точка вращения в пикселях модели (X уже зеркалирован). */
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        /** Текущая поза в радианах / пикселях; анимация перезаписывает её каждый кадр. */
        public float rotationX;
        public float rotationY;
        public float rotationZ;
        public float positionX;
        public float positionY;
        public float positionZ;
        public float scaleX = 1.0f;
        public float scaleY = 1.0f;
        public float scaleZ = 1.0f;

        public Bone(String name) {
            this.name = name;
        }

        Bone find(String target) {
            if (this.name.equals(target)) {
                return this;
            }
            for (Bone child : this.childBones) {
                Bone found = child.find(target);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
    }

    public static final class Cube {
        /** Грани по порядку: west, east, down, up, north, south; отсутствующая грань — null. */
        public final Quad[] quads = new Quad[6];
        public final float sizeX;
        public final float sizeY;
        public final float sizeZ;
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public float rotationX;
        public float rotationY;
        public float rotationZ;

        public Cube(float sizeX, float sizeY, float sizeZ) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    public static final class Quad {
        public final Vertex[] vertices;
        public final float normalX;
        public final float normalY;
        public final float normalZ;

        public Quad(Vertex[] vertices, float normalX, float normalY, float normalZ) {
            this.vertices = vertices;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }
    }

    public static final class Vertex {
        public final float x;
        public final float y;
        public final float z;
        public final float u;
        public final float v;

        public Vertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }
    }
}
