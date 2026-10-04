/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.common.animation.inovated;

public class EasingList {
    public static final double c1 = 1.70158;
    public static final double c2 = 2.5949095;
    public static final double c3 = 2.70158;
    public static final double c4 = 2.0943951023931953;
    public static final double c5 = 1.3962634015954636;
    public static final Easing QUAD_IN_OUT = EasingList.powIN_OUT(2.0);
    public static final Easing SINE_IN = value -> (float)(1.0 - Math.cos((double)value * Math.PI / 2.0));
    public static final Easing SINE_OUT = value -> (float)Math.sin((double)value * Math.PI / 2.0);
    public static final Easing SINE_BOTH = value -> (float)(-(Math.cos(Math.PI * (double)value) - 1.0) / 2.0);
    public static final Easing CIRC_IN = value -> (float)(1.0 - Math.sqrt(1.0 - Math.pow(value, 2.0)));
    public static final Easing CIRC_OUT = value -> (float)Math.sqrt(1.0 - Math.pow((double)value - 1.0, 2.0));
    public static final Easing CIRC_BOTH = value -> (float)((double)value < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * (double)value, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * (double)value + 2.0, 2.0)) + 1.0) / 2.0);
    public static final Easing ELASTIC_IN = value -> (double)value != 0.0 && (double)value != 1.0 ? (float)(Math.pow(-2.0, 10.0 * (double)value - 10.0) * Math.sin(((double)value * 10.0 - 10.75) * 2.0943951023931953)) : value;
    public static final Easing ELASTIC_OUT = value -> (double)value != 0.0 && (double)value != 1.0 ? (float)(Math.pow(2.0, -10.0 * (double)value) * Math.sin(((double)value * 10.0 - 0.75) * 2.0943951023931953) + 1.0) : value;
    public static final Easing ELASTIC_BOTH = value -> {
        if ((double)value != 0.0 && (double)value != 1.0) {
            return (float)((double)value < 0.5 ? -(Math.pow(2.0, 20.0 * (double)value - 10.0) * Math.sin((20.0 * (double)value - 11.125) * 1.3962634015954636)) / 2.0 : Math.pow(2.0, -20.0 * (double)value + 10.0) * Math.sin((20.0 * (double)value - 11.125) * 1.3962634015954636) / 2.0 + 1.0);
        }
        return value;
    };
    public static final Easing EXPO_IN = value -> (double)value != 0.0 ? (float)Math.pow(2.0, 10.0 * (double)value - 10.0) : value;
    public static final Easing EXPO_OUT = value -> (double)value != 1.0 ? (float)(1.0 - Math.pow(2.0, -10.0 * (double)value)) : value;
    public static final Easing EXPO_BOTH = value -> {
        if ((double)value != 0.0 && (double)value != 1.0) {
            return (float)((double)value < 0.5 ? Math.pow(2.0, 20.0 * (double)value - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * (double)value + 10.0)) / 2.0);
        }
        return value;
    };
    public static final Easing BACK_IN = value -> (float)(2.70158 * Math.pow(value, 3.0) - 1.70158 * Math.pow(value, 2.0));
    public static final Easing BACK_OUT = value -> (float)(1.0 + 2.70158 * Math.pow((double)value - 1.0, 3.0) + 1.70158 * Math.pow((double)value - 1.0, 2.0));
    public static final Easing NONE = value -> value;
    public static final Easing BACK_BOTH = value -> (float)((double)value < 0.5 ? Math.pow(2.0 * (double)value, 2.0) * (7.189819 * (double)value - 2.5949095) / 2.0 : (Math.pow(2.0 * (double)value - 2.0, 2.0) * (3.5949095 * ((double)value * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0);
    public static final Easing BOUNCE_OUT = value -> {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if ((double)value < 1.0 / (double)d1) {
            return (float)((double)n1 * Math.pow(value, 2.0));
        }
        if ((double)value < 2.0 / (double)d1) {
            return (float)((double)n1 * Math.pow((double)value - 1.5 / (double)d1, 2.0) + 0.75);
        }
        return (float)((double)value < 2.5 / (double)d1 ? (double)n1 * Math.pow((double)value - 2.25 / (double)d1, 2.0) + 0.9375 : (double)n1 * Math.pow((double)value - 2.625 / (double)d1, 2.0) + 0.984375);
    };
    public static final Easing BOUNCE_IN = value -> (float)(1.0 - (double)BOUNCE_OUT.ease((float)(1.0 - (double)value)));
    public static final Easing BOUNCE_BOTH = value -> (float)((double)value < 0.5 ? (1.0 - (double)BOUNCE_OUT.ease((float)(1.0 - 2.0 * (double)value))) / 2.0 : (1.0 + (double)BOUNCE_OUT.ease((float)(2.0 * (double)value - 1.0))) / 2.0);
    public static final Easing QUINT_IN = x -> (double)x < 0.5 ? 16.0f * x * x * x * x * x : (float)(1.0 - Math.pow(-2.0f * x + 2.0f, 5.0) / 2.0);

    public static Easing powIN_OUT(double n) {
        return value -> (float)((double)value < 0.5 ? Math.pow(2.0, n - 1.0) * Math.pow(value, n) : 1.0 - Math.pow(-2.0 * (double)value + 2.0, n) / 2.0);
    }

    @FunctionalInterface
    public static interface Easing {
        public float ease(float var1);
    }
}
