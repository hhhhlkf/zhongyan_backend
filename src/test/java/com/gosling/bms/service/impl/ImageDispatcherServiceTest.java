package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.utils.PolyCoordinateUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageDispatcherServiceTest {

    @Test
    void shouldCalculateVerticalFovFromAspectRatio() {
        double verticalFovDeg = ImageDispatcherService.calcVerticalFovDeg(60.0, 4000, 3000);

        assertEquals(46.8264, verticalFovDeg, 1e-3);
    }

    @Test
    void shouldRotateOffsetsByYaw() {
        PolyCoordinateUtils.GeoPoint rotated = ImageDispatcherService.offsetByYaw(30.0, 114.0, 100.0, 0.0, 90.0);
        PolyCoordinateUtils.GeoPoint north = ImageDispatcherService.moveLatLon(30.0, 114.0, 0.0, -100.0);

        assertEquals(north.getLon(), rotated.getLon(), 1e-6);
        assertEquals(north.getLat(), rotated.getLat(), 1e-6);
    }

    @Test
    void shouldUseConfiguredVerticalFovFirst() throws Exception {
        DataConfig.Coordinate coordinate = new DataConfig.Coordinate();
        coordinate.setVerticalFovDeg(33.0);

        double verticalFovDeg = ImageDispatcherService.resolveVerticalFovDeg(coordinate, null, 60.0);

        assertEquals(33.0, verticalFovDeg, 1e-9);
    }

    @Test
    void shouldMoveLatLonInExpectedDirection() {
        PolyCoordinateUtils.GeoPoint point = ImageDispatcherService.moveLatLon(30.0, 114.0, 100.0, 100.0);

        assertTrue(point.getLon() > 114.0);
        assertTrue(point.getLat() > 30.0);
    }

    @Test
    void shouldExtractRgbImageSuffixAndRecognizeTimestampTxt() throws Exception {
        Path txtFile = Files.createTempFile("123", ".txt");
        Path jpgFile = Files.createTempFile("rgb_123", ".JPG");
        assertEquals("123", ImageDispatcherService.extractImageSuffix("rgb_123.JPG"));
        assertEquals("123", ImageDispatcherService.extractImageSuffix("rgbO_123.png"));
        assertTrue(ImageDispatcherService.isTimestampTxtFile(txtFile.toFile()));
        assertFalse(ImageDispatcherService.isTimestampTxtFile(jpgFile.toFile()));
        Files.deleteIfExists(txtFile);
        Files.deleteIfExists(jpgFile);
    }

    @Test
    void shouldReadThirteenDigitTimestampFile() throws Exception {
        Path timeFile = Files.createTempFile("capture-time", ".txt");
        Files.writeString(timeFile, "1712723456789");

        long timestamp = ImageDispatcherService.readCaptureTimestampMillis(timeFile);

        assertEquals(1712723456789L, timestamp);
        Files.deleteIfExists(timeFile);
    }

    @Test
    void shouldNormalizeTelemetryTimestampToMillis() {
        assertEquals(1712723456000L, ImageDispatcherService.normalizeTimestampMillis(1712723456L));
        assertEquals(1712723456789L, ImageDispatcherService.normalizeTimestampMillis(1712723456789L));
    }

    @Test
    void shouldBuildSameTimestampedGroupNameForCollectAndProcessImages() {
        assertEquals("rgb_123_1712723456789.JPG",
                ImageDispatcherService.buildLocalFileName("rgb_123.JPG", 1712723456789L));
        assertEquals("rgbO_123_1712723456789.png",
                ImageDispatcherService.buildLocalFileName("rgbO_123.png", 1712723456789L));
    }

    @Test
    void shouldResolveSamePolyNameForTimestampedCollectAndProcessImages() {
        assertEquals("rgbP_123_1712723456789.txt", FileData.getPolyFileName("rgb_123_1712723456789.JPG"));
        assertEquals("rgbP_123_1712723456789.txt", FileData.getPolyFileName("rgbO_123_1712723456789.png"));
    }

    @Test
    void shouldFallbackMatchOldProcessImageToTimestampedPoly() throws Exception {
        Path polyDir = Files.createTempDirectory("poly-dir");
        Path polyFile = polyDir.resolve("rgbP_123_1712723456789.txt");
        Files.writeString(polyFile, "1 1");

        assertEquals(polyFile.toFile(), FileData.findMatchingPolyFile(polyDir.toFile(), "rgbO_123.png"));

        Files.deleteIfExists(polyFile);
        Files.deleteIfExists(polyDir);
    }

    @Test
    void shouldMaterializeMissingCollectPolyFromSharedProcessPoly() throws Exception {
        Path typeDir = Files.createTempDirectory("rgb-type");
        Path collectDir = typeDir.resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Path processPolyDir = typeDir.resolve("process").resolve("poly");
        Files.createDirectories(collectPolyDir);
        Files.createDirectories(processPolyDir);
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(processPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));

        ImageDispatcherService.reconcileTaskPolyDirectory(collectDir.toFile(), collectPolyDir.toFile());

        assertTrue(Files.exists(collectPolyDir.resolve("rgbP_000001.txt")));
        assertEquals(List.of("1 1", "2 2"), Files.readAllLines(collectPolyDir.resolve("rgbP_000001.txt")));
    }

    @Test
    void shouldRemoveOrphanCollectPolyDuringReconcile() throws Exception {
        Path typeDir = Files.createTempDirectory("rgb-type-prune");
        Path collectDir = typeDir.resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Path processPolyDir = typeDir.resolve("process").resolve("poly");
        Files.createDirectories(collectPolyDir);
        Files.createDirectories(processPolyDir);
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(collectPolyDir.resolve("rgbP_999999.txt"), List.of("9 9", "8 8"));
        Files.write(processPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));

        ImageDispatcherService.reconcileTaskPolyDirectory(collectDir.toFile(), collectPolyDir.toFile());

        assertTrue(Files.exists(collectPolyDir.resolve("rgbP_000001.txt")));
        assertFalse(Files.exists(collectPolyDir.resolve("rgbP_999999.txt")));
    }
}
