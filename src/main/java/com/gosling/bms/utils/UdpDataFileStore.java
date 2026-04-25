package com.gosling.bms.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class UdpDataFileStore {

    public static final Path DEFAULT_FILE_PATH = Paths.get("src/main/resources/static/udp_data.json");

    private static final Object FILE_LOCK = new Object();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UdpDataFileStore() {
    }

    public static void append(String data, int maxRecords) throws IOException {
        append(DEFAULT_FILE_PATH, data, maxRecords);
    }

    public static void append(Path filePath, String data, int maxRecords) throws IOException {
        synchronized (FILE_LOCK) {
            ArrayNode current = readArrayNode(filePath);
            JsonNode newNode = OBJECT_MAPPER.readTree(data);
            current.add(newNode);
            writeArrayAtomically(filePath, keepLatest(current, maxRecords));
        }
    }

    public static void trimToLatest(int maxRecords) throws IOException {
        trimToLatest(DEFAULT_FILE_PATH, maxRecords);
    }

    public static void trimToLatest(Path filePath, int maxRecords) throws IOException {
        synchronized (FILE_LOCK) {
            ArrayNode current = readArrayNode(filePath);
            ArrayNode latest = keepLatest(current, maxRecords);
            if (latest.size() != current.size()) {
                writeArrayAtomically(filePath, latest);
            }
        }
    }

    public static ArrayNode readSnapshot() throws IOException {
        return readSnapshot(DEFAULT_FILE_PATH);
    }

    public static ArrayNode readSnapshot(Path filePath) throws IOException {
        synchronized (FILE_LOCK) {
            return readArrayNode(filePath).deepCopy();
        }
    }

    public static JsonNode readLatest() throws IOException {
        return readLatest(DEFAULT_FILE_PATH);
    }

    public static JsonNode readLatest(Path filePath) throws IOException {
        synchronized (FILE_LOCK) {
            ArrayNode arrayNode = readArrayNode(filePath);
            if (arrayNode.size() == 0) {
                return null;
            }
            return arrayNode.get(arrayNode.size() - 1).deepCopy();
        }
    }

    static ArrayNode keepLatest(ArrayNode source, int maxRecords) {
        ArrayNode latest = OBJECT_MAPPER.createArrayNode();
        if (maxRecords <= 0 || source == null || source.size() == 0) {
            return latest;
        }
        int start = Math.max(source.size() - maxRecords, 0);
        for (int i = start; i < source.size(); i++) {
            latest.add(source.get(i));
        }
        return latest;
    }

    private static ArrayNode readArrayNode(Path filePath) throws IOException {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            return OBJECT_MAPPER.createArrayNode();
        }

        JsonNode root = OBJECT_MAPPER.readTree(filePath.toFile());
        if (root == null || root.isNull()) {
            return OBJECT_MAPPER.createArrayNode();
        }
        if (!root.isArray()) {
            throw new IOException("udp_data.json format is invalid");
        }
        return (ArrayNode) root;
    }

    private static void writeArrayAtomically(Path filePath, ArrayNode data) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = Files.createTempFile(parent, filePath.getFileName().toString(), ".tmp");
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), data);
            try {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
