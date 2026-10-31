package com.nationdroid.launcher;

/** Stable Fibonacci distribution and rotations shared by rendering and regression tests. */
public final class SphereMath {
    private SphereMath() {}
    public static double[] point(int index, int count) {
        if (count <= 0 || index < 0 || index >= count) throw new IllegalArgumentException("Invalid sphere index");
        double y = 1 - 2 * (index + .5) / count;
        double radius = Math.sqrt(Math.max(0, 1 - y * y));
        double angle = index * Math.PI * (3 - Math.sqrt(5));
        return new double[]{Math.cos(angle) * radius, y, Math.sin(angle) * radius};
    }
    public static void rotate(double[] point, double yaw, double pitch, double[] out) {
        double x = point[0] * Math.cos(yaw) + point[2] * Math.sin(yaw);
        double z = -point[0] * Math.sin(yaw) + point[2] * Math.cos(yaw);
        out[0] = x;
        out[1] = point[1] * Math.cos(pitch) - z * Math.sin(pitch);
        out[2] = point[1] * Math.sin(pitch) + z * Math.cos(pitch);
    }
    public static double damping(double velocity, double strength, double seconds) {
        return velocity * Math.exp(-Math.max(.1, strength) * Math.max(0, seconds));
    }
    public static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); }
}
