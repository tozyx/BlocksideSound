package com.tozyx.blocksidesound.client.rain;

import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalDouble;

/** Reads the selected resource's Vorbis sample count without decoding audio or trusting a sidecar. */
public final class OggDuration {
    private OggDuration() {}

    public static OptionalDouble seconds(InputStream input) throws IOException {
        long samples = -1;
        int rate = 0;
        int serial = 0;
        int bytes = 0;
        while (bytes <= 8 * 1024 * 1024) {
            byte[] header = input.readNBytes(27);
            if (header.length == 0) break;
            if (header.length != 27 || header[0] != 'O' || header[1] != 'g'
                    || header[2] != 'g' || header[3] != 'S' || header[4] != 0) return OptionalDouble.empty();
            int pageSerial = littleInt(header, 14);
            if (bytes == 0) serial = pageSerial;
            else if (serial != pageSerial) return OptionalDouble.empty(); // Chained/non-Vorbis streams fall back safely.
            int count = header[26] & 255;
            byte[] lacing = input.readNBytes(count);
            if (lacing.length != count) return OptionalDouble.empty();
            int size = 0;
            for (byte segment : lacing) size += segment & 255;
            byte[] packet = input.readNBytes(size);
            if (packet.length != size) return OptionalDouble.empty();
            if (bytes == 0) {
                if (packet.length < 16 || packet[0] != 1 || packet[1] != 'v' || packet[2] != 'o'
                        || packet[3] != 'r' || packet[4] != 'b' || packet[5] != 'i' || packet[6] != 's') {
                    return OptionalDouble.empty();
                }
                rate = littleInt(packet, 12);
                if (rate <= 0 || rate > 384000) return OptionalDouble.empty();
            }
            long granule = 0;
            for (int i = 7; i >= 0; i--) granule = (granule << 8) | (header[6 + i] & 255L);
            if (granule >= 0) samples = granule;
            bytes += 27 + count + size;
        }
        return bytes <= 8 * 1024 * 1024 && rate > 0 && samples > 0
                ? OptionalDouble.of(samples / (double) rate) : OptionalDouble.empty();
    }

    private static int littleInt(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | (bytes[offset + 1] & 255) << 8
                | (bytes[offset + 2] & 255) << 16 | (bytes[offset + 3] & 255) << 24;
    }
}
