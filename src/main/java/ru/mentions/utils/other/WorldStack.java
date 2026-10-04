package ru.mentions.utils.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Матричный стек в том виде, в каком его ждут модули mentions: только вращение камеры, без сдвига на её позицию.
 * (Стек события мира Wios уже сдвинут на позицию камеры.)
 */
public final class WorldStack {

    private WorldStack() {}

    public static MatrixStack create() {
        MatrixStack stack = new MatrixStack();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        stack.multiplyPositionMatrix(new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf())));
        return stack;
    }
}
