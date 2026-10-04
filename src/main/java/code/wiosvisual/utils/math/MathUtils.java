/*
 * Decompiled with CFR 0.152.
 */
package code.wiosvisual.utils.math;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
    public static float random(float min, float max) {
        SecureRandom secureRandom = new SecureRandom();
        double randA = secureRandom.nextDouble();
        double randB = secureRandom.nextDouble();
        double randC = secureRandom.nextGaussian() * (double)0.02f;
        double smoothFactor = Math.pow(randA, 1.0 + secureRandom.nextDouble() * 0.7);
        double mixFactor = (randB * 0.8 + 0.1) * (Math.log1p(randA * 3.0) * 0.5 + 0.5);
        return (float)((double)min + (double)(max - min) * smoothFactor * mixFactor + randC);
    }

    public static double randomBest(double min, double max) {
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    private MathUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
