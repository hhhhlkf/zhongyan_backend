package com.gosling.bms.utils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PolyCoordinateUtilsTest {

    @Test
    void shouldConvertFourCornersToBoundingBox() {
        List<PolyCoordinateUtils.GeoPoint> points = List.of(
                new PolyCoordinateUtils.GeoPoint(113.0, 30.2),
                new PolyCoordinateUtils.GeoPoint(113.4, 30.1),
                new PolyCoordinateUtils.GeoPoint(113.3, 29.8),
                new PolyCoordinateUtils.GeoPoint(112.9, 29.9)
        );

        double[] bbox = PolyCoordinateUtils.toLeftTopRightBottomArray(points);

        assertArrayEquals(new double[]{112.9, 30.2, 113.4, 29.8}, bbox, 1e-9);
    }

    @Test
    void shouldReadAndWriteCornerPoints() throws Exception {
        File tempFile = Files.createTempFile("poly-corners", ".txt").toFile();
        List<PolyCoordinateUtils.GeoPoint> points = List.of(
                new PolyCoordinateUtils.GeoPoint(120.1234567, 31.1234567),
                new PolyCoordinateUtils.GeoPoint(120.2234567, 31.0234567),
                new PolyCoordinateUtils.GeoPoint(120.3234567, 30.9234567),
                new PolyCoordinateUtils.GeoPoint(120.0234567, 31.0034567)
        );

        PolyCoordinateUtils.writePoints(tempFile, points);
        List<PolyCoordinateUtils.GeoPoint> reloaded = PolyCoordinateUtils.readPoints(tempFile);

        assertEquals(4, reloaded.size());
        assertEquals(points.get(0).getLon(), reloaded.get(0).getLon(), 1e-7);
        assertEquals(points.get(2).getLat(), reloaded.get(2).getLat(), 1e-7);
    }
}
