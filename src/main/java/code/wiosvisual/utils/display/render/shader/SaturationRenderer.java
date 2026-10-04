package code.wiosvisual.utils.display.render.shader;

/** Полноэкранный пост-эффект насыщенности/vibrance для Ambience. */
public final class SaturationRenderer {
    private static SaturationPipeline pipeline;

    private SaturationRenderer() {
    }

    public static void render(float saturation, float vibrance) {
        if (pipeline == null) {
            pipeline = new SaturationPipeline();
        }
        pipeline.draw(saturation, vibrance);
    }
}
