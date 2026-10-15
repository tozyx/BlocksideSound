package com.tozyx.blocksidesound.client.rain;

/** Short onset and invalid-surface release fades; natural file endings are never timed or cut off. */
final class MaterialEnvelope {
    static final int FADE_TICKS = 8;
    static final int ATTACK_TICKS = 2;
    private final boolean sustained;
    private int attackAge;
    private int releaseAge = -1;
    private float releaseGain;

    MaterialEnvelope(boolean sustained) {
        this.sustained = sustained;
    }

    float tick() {
        attackAge = Math.min(ATTACK_TICKS, attackAge + 1);
        if (releaseAge >= 0) releaseAge = Math.min(FADE_TICKS, releaseAge + 1);
        return gain();
    }

    float gain() {
        if (releaseAge >= 0) return releaseGain * curve(1.0F - releaseAge / (float) FADE_TICKS);
        return sustained ? curve(attackAge / (float) ATTACK_TICKS) : 1;
    }

    private static float curve(float progress) {
        return (float) Math.sin(Math.clamp(progress, 0, 1) * Math.PI / 2);
    }

    void release() {
        if (releaseAge >= 0) return;
        releaseGain = gain();
        releaseAge = 0;
    }

    boolean releasing() { return releaseAge >= 0; }

    boolean finished() {
        return releaseAge >= FADE_TICKS;
    }
}
