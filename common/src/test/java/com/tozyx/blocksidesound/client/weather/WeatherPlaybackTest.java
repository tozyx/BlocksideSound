package com.tozyx.blocksidesound.client.weather;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeatherPlaybackTest {
    @Test void nativeThunderMagnitudeCannotSaturateModVolumeSlider() {
        assertEquals(0.25F, ThunderSound.gain(10000, 0.25));
        assertEquals(0.125F, ThunderSound.gain(0.5F, 0.25));
        assertEquals(0, ThunderSound.gain(10000, 0));
        assertEquals(1, ThunderSound.gain(10000, 20));
        assertEquals(0, ThunderSound.gain(Float.NaN, 1));
        assertEquals(0, ThunderSound.gain(1, Double.POSITIVE_INFINITY));
    }

    @Test void thunderRangeIsNativeAndIndependentOfUserGain() {
        assertEquals(160000, ThunderSound.range(10000, 16));
        assertEquals(16, ThunderSound.range(0.1F, 16));
        assertEquals(1600, ThunderSound.range(100, 16));
        assertEquals(Integer.MAX_VALUE, ThunderSound.range(Float.MAX_VALUE, 256));
        assertEquals(1, ThunderSound.range(-1, 0));
        assertEquals(16, ThunderSound.range(Float.NaN, 16));
    }

    @Test void thunderProfilesFollowDistanceBoundaries() {
        assertEquals("blocksidesound:weather.thunder.close", ThunderSound.profile(0).toString());
        assertEquals("blocksidesound:weather.thunder.close", ThunderSound.profile(4095).toString());
        assertEquals("blocksidesound:weather.thunder.medium", ThunderSound.profile(64 * 64).toString());
        assertEquals("blocksidesound:weather.thunder.medium", ThunderSound.profile(160 * 160 - 1).toString());
        assertEquals("blocksidesound:weather.thunder.far", ThunderSound.profile(160 * 160).toString());
    }
}
