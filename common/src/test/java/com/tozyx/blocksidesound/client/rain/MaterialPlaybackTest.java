package com.tozyx.blocksidesound.client.rain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterialPlaybackTest {
    @Test void aFullTakeHasOnlyAShortOnsetAndNoDurationBasedFadeOrStop() {
        MaterialEnvelope envelope = new MaterialEnvelope(true);
        assertEquals(0, envelope.gain());
        for (int i = 1; i <= 2; i++) assertEquals(curve(i / 2F), envelope.tick(), 1.0e-6);
        for (int i = 0; i < 100_000; i++) {
            assertEquals(1, envelope.tick());
            assertFalse(envelope.finished());
        }
    }

    @Test void repeatedReleaseDoesNotRestartOrExtendFade() {
        MaterialEnvelope envelope = new MaterialEnvelope(true);
        for (int i = 0; i < 8; i++) envelope.tick();
        for (int i = 0; i < 8; i++) {
            envelope.release();
            assertEquals(curve(1 - (i + 1) / 8F), envelope.tick(), 1.0e-6);
        }
        assertTrue(envelope.finished());
    }

    @Test void aDripKeepsItsAttack() {
        MaterialEnvelope drip = new MaterialEnvelope(false);
        assertEquals(1, drip.gain());
        for (int i = 0; i < 8; i++) assertEquals(1, drip.tick());
        drip.release();
        assertTrue(drip.tick() < 1);
    }

    @Test void everyMaterialAndDripIsAnIndependentNonLoopingTake() {
        var metal = new MaterialRainSound(RainSound.METAL, 1, 1,
                net.minecraft.util.RandomSource.create(1), net.minecraft.world.phys.Vec3.ZERO);
        var drip = new MaterialRainSound(RainSound.PLANT_DRIP, 1, 1,
                net.minecraft.util.RandomSource.create(2), net.minecraft.world.phys.Vec3.ZERO);
        assertFalse(metal.isLooping());
        assertEquals(0, metal.getDelay());
        assertFalse(drip.isLooping());
        assertEquals(net.minecraft.sounds.SoundSource.WEATHER, metal.getSource());
        for (int i = 0; i < 10_000; i++) metal.tick();
        assertFalse(metal.isStopped(), "Voice lifetime is controlled by the native decoder, not guessed EOF ticks");
        assertEquals(1, metal.getVolume());
    }

    @Test void aPlayingTakeFollowsRainIntensityWithoutRestarting() {
        var metal = new MaterialRainSound(RainSound.METAL, .01F, 1,
                net.minecraft.util.RandomSource.create(3), net.minecraft.world.phys.Vec3.ZERO);
        for (int i = 0; i < 8; i++) metal.tick();
        assertEquals(.01F, metal.getVolume(), 1.0e-6);
        metal.gain(1);
        metal.tick();
        assertEquals(1, metal.getVolume());
        metal.gain(.4F);
        metal.tick();
        assertEquals(.4F, metal.getVolume(), 1.0e-6);
        assertFalse(metal.isStopped());
        assertFalse(metal.isLooping());
    }

    @Test void startingANewTakeDoesNotReleaseOrReduceTheOlderTake() {
        var older=new MaterialRainSound(RainSound.METAL,.5F,1,
                net.minecraft.util.RandomSource.create(4),net.minecraft.world.phys.Vec3.ZERO);
        for(int i=0;i<30;i++) older.tick();
        var newer=new MaterialRainSound(RainSound.METAL,.5F,1,
                net.minecraft.util.RandomSource.create(5),net.minecraft.world.phys.Vec3.ZERO);
        for(int i=0;i<2;i++) { older.tick();newer.tick(); }
        assertEquals(.5F,older.getVolume());
        assertEquals(.5F,newer.getVolume());
        assertFalse(older.releasing());
        assertFalse(older.isStopped());
    }

    @Test void overlapGainIsFixedPerTakeAndUsesDurationOnlyForEnergyCompensation() {
        assertEquals(Math.sqrt(1.6/5),RainPlayer.layerGain(java.util.OptionalDouble.of(5),1),1e-6);
        assertEquals(Math.sqrt(1.6/3),RainPlayer.layerGain(java.util.OptionalDouble.of(3),1),1e-6);
        assertTrue(Float.isFinite(RainPlayer.layerGain(java.util.OptionalDouble.empty(),1)));
        assertTrue(RainPlayer.layerGain(java.util.OptionalDouble.of(7),.95F)<
                RainPlayer.layerGain(java.util.OptionalDouble.of(7),1.05F));
    }

    private static float curve(float value) {
        return (float) Math.sin(value * Math.PI / 2);
    }

    @Test void durationUsesTheLastSampleGranule() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] id = new byte[30];
        id[0] = 1;
        System.arraycopy(new byte[]{'v', 'o', 'r', 'b', 'i', 's'}, 0, id, 1, 6);
        id[11] = 1;
        int rate = 44100;
        for (int i = 0; i < 4; i++) id[12 + i] = (byte) (rate >>> (8 * i));
        page(stream, id, 0);
        page(stream, new byte[]{0}, 5L * rate);
        assertEquals(5, OggDuration.seconds(new ByteArrayInputStream(stream.toByteArray())).orElseThrow());
    }

    @Test void invalidOrTruncatedMetadataDoesNotInventADuration() throws IOException {
        assertTrue(OggDuration.seconds(new ByteArrayInputStream(new byte[0])).isEmpty());
        assertTrue(OggDuration.seconds(new ByteArrayInputStream(new byte[]{'O', 'g', 'g'})).isEmpty());
        assertTrue(OggDuration.seconds(new ByteArrayInputStream(new byte[27])).isEmpty());
    }

    @Test void bundledMaterialsAreIndependentFullOriginalTakes() throws IOException {
        for (String kind : new String[]{"wood", "metal", "glass", "foliage", "granular", "water", "soft"}) {
            for (int i = 1; i <= 3; i++) {
                try (var input = getClass().getResourceAsStream(
                        "/assets/blocksidesound/sounds/rain/" + kind + "_0" + i + ".ogg")) {
                    assertNotNull(input);
                    double seconds = OggDuration.seconds(input).orElseThrow();
                    assertTrue(seconds >= 3 && seconds < 10,
                            kind + " must be an independent original take, not a synthesized loop");
                }
            }
        }
    }

    private static void page(ByteArrayOutputStream output, byte[] payload, long samples) throws IOException {
        byte[] header = new byte[28];
        header[0] = 'O'; header[1] = 'g'; header[2] = 'g'; header[3] = 'S';
        for (int i = 0; i < 8; i++) header[6 + i] = (byte) (samples >>> (8 * i));
        header[26] = 1; header[27] = (byte) payload.length;
        output.write(header);
        output.write(payload);
    }
}
