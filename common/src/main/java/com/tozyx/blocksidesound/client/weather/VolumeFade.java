package com.tozyx.blocksidesound.client.weather;

/** A bounded, reversible fade so a change of shelter does not restart the rain abruptly. */
final class VolumeFade {
    private float value;
    private float target;

    void target(float target) {
        this.target = Float.isFinite(target) ? Math.clamp(target, 0.0F, 1.0F) : 0;
    }

    float tick() {
        float difference = target - value;
        value = Math.abs(difference) <= 0.05F ? target : value + Math.copySign(0.05F, difference);
        return value;
    }

    boolean silent() {
        return value == 0 && target == 0;
    }
}
