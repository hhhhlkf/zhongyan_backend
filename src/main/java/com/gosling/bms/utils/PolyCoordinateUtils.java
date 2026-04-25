package com.gosling.bms.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PolyCoordinateUtils {

    private PolyCoordinateUtils() {
    }

    public static List<GeoPoint> readPoints(File polyFile) throws IOException {
        if (polyFile == null || !polyFile.exists()) {
            return Collections.emptyList();
        }
        List<String> lines = Files.readAllLines(polyFile.toPath(), StandardCharsets.UTF_8);
        List<GeoPoint> points = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] arr = line.trim().split("\\s+|,");
            if (arr.length != 2) {
                continue;
            }
            points.add(new GeoPoint(Double.parseDouble(arr[0]), Double.parseDouble(arr[1])));
        }
        return points;
    }

    public static void writePoints(File polyFile, List<GeoPoint> points) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(polyFile.toPath(), StandardCharsets.UTF_8)) {
            for (GeoPoint point : points) {
                writer.write(String.format(Locale.US, "%.7f %.7f", point.getLon(), point.getLat()));
                writer.newLine();
            }
        }
    }

    public static double[] toLeftTopRightBottomArray(List<GeoPoint> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("poly points must not be empty");
        }
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        for (GeoPoint point : points) {
            minLon = Math.min(minLon, point.getLon());
            maxLon = Math.max(maxLon, point.getLon());
            minLat = Math.min(minLat, point.getLat());
            maxLat = Math.max(maxLat, point.getLat());
        }
        return new double[]{minLon, maxLat, maxLon, minLat};
    }

    public static Float[] toLeftTopRightBottomFloatArray(List<GeoPoint> points) {
        double[] bbox = toLeftTopRightBottomArray(points);
        return new Float[]{
                (float) bbox[0],
                (float) bbox[1],
                (float) bbox[2],
                (float) bbox[3]
        };
    }

    public static final class GeoPoint {
        private final double lon;
        private final double lat;

        public GeoPoint(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public double getLat() {
            return lat;
        }
    }
}
