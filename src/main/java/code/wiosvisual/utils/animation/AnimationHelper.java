/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.animation;

import java.util.HashMap;
import java.util.Map;

public class AnimationHelper {
    private static final Map<String, AnimationData> animations = new HashMap<String, AnimationData>();

    public static void startAnimation(String key, float startValue, float endValue, long duration, EasingType easingType) {
        animations.put(key, new AnimationData(startValue, endValue, duration, easingType));
    }

    public static float getAnimationValue(String key, float defaultValue) {
        AnimationData data = animations.get(key);
        if (data == null) {
            return defaultValue;
        }
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - data.startTime;
        if (elapsed >= data.duration) {
            animations.remove(key);
            return data.endValue;
        }
        float progress = (float)elapsed / (float)data.duration;
        float easedProgress = AnimationHelper.applyEasing(progress, data.easingType);
        return data.startValue + (data.endValue - data.startValue) * easedProgress;
    }

    public static boolean isAnimationActive(String key) {
        return animations.containsKey(key);
    }

    public static void stopAnimation(String key) {
        animations.remove(key);
    }

    private static float applyEasing(float t, EasingType easingType) {
        switch (easingType.ordinal()) {
            case 0: {
                return t;
            }
            case 1: {
                return 1.0f - (1.0f - t) * (1.0f - t);
            }
            case 2: {
                return t * t;
            }
            case 3: {
                return t < 0.5f ? 2.0f * t * t : 1.0f - (2.0f - 2.0f * t) * (2.0f - 2.0f * t) / 2.0f;
            }
            case 4: {
                if (t < 0.36363637f) {
                    return 7.5625f * t * t;
                }
                if (t < 0.72727275f) {
                    return 7.5625f * (t -= 0.54545456f) * t + 0.75f;
                }
                if (t < 0.90909094f) {
                    return 7.5625f * (t -= 0.8181818f) * t + 0.9375f;
                }
                return 7.5625f * (t -= 0.95454544f) * t + 0.984375f;
            }
        }
        return t;
    }

    public static void fadeIn(String key, long duration) {
        AnimationHelper.startAnimation(key, 0.0f, 1.0f, duration, EasingType.EASE_OUT);
    }

    public static void fadeOut(String key, long duration) {
        AnimationHelper.startAnimation(key, 1.0f, 0.0f, duration, EasingType.EASE_IN);
    }

    public static void slideIn(String key, float startY, float endY, long duration) {
        AnimationHelper.startAnimation(key, startY, endY, duration, EasingType.BOUNCE_OUT);
    }

    public static void colorTransition(String key, int startColor, int endColor, long duration) {
        AnimationHelper.startAnimation(key, startColor, endColor, duration, EasingType.EASE_IN_OUT);
    }

    public static class AnimationData {
        public float startValue;
        public float endValue;
        public long startTime;
        public long duration;
        public EasingType easingType;

        public AnimationData(float startValue, float endValue, long duration, EasingType easingType) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.easingType = easingType;
        }
    }

    public static enum EasingType {
        LINEAR,
        EASE_OUT,
        EASE_IN,
        EASE_IN_OUT,
        BOUNCE_OUT;

    }
}
