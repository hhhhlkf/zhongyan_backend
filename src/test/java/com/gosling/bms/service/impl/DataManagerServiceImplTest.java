package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.DeleteItemsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataManagerServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void transferFileShouldClearPreviewDirectoryOnly() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path collectDir = tempDir.resolve("rgb").resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Path historyDir = tempDir.resolve("rgb").resolve("history");

        Files.createDirectories(collectPolyDir);
        Files.createDirectories(historyDir.resolve("poly"));
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(collectPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));
        Files.write(historyDir.resolve("rgb_000001.png"), new byte[]{4, 5, 6});

        assertTrue(service.transferFile("rgb", "collect"));

        assertFalse(Files.exists(collectDir.resolve("rgb_000001.jpg")));
        assertFalse(Files.exists(collectPolyDir.resolve("rgbP_000001.txt")));
        assertTrue(Files.exists(historyDir.resolve("rgb_000001.png")));
    }

    @Test
    void deleteHistoryFileShouldDeleteImageAndPoly() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path historyDir = tempDir.resolve("rgb").resolve("history");
        Path historyPolyDir = historyDir.resolve("poly");
        Files.createDirectories(historyPolyDir);
        Files.write(historyDir.resolve("rgb_000001.png"), new byte[]{1, 2, 3});
        Files.write(historyPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));

        assertTrue(service.deleteHistoryFile("rgb", "rgb_000001.png"));
        assertFalse(Files.exists(historyDir.resolve("rgb_000001.png")));
        assertFalse(Files.exists(historyPolyDir.resolve("rgbP_000001.txt")));
        assertFalse(service.deleteHistoryFile("rgb", "rgb_000001.png"));
    }

    @Test
    void deleteItemsShouldDeleteCollectFilesAndPoly() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path collectDir = tempDir.resolve("rgb").resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Files.createDirectories(collectPolyDir);
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(collectPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));

        DeleteItemsResult result = service.deleteItems("rgb", "collect", List.of("rgb_000001.jpg"));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertFalse(Files.exists(collectDir.resolve("rgb_000001.jpg")));
        assertFalse(Files.exists(collectPolyDir.resolve("rgbP_000001.txt")));
    }

    @Test
    void deleteItemsShouldDeleteHistoryFilesThroughUnifiedEntry() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path historyDir = tempDir.resolve("rgb").resolve("history");
        Path historyPolyDir = historyDir.resolve("poly");
        Files.createDirectories(historyPolyDir);
        Files.write(historyDir.resolve("rgb_000001.png"), new byte[]{1, 2, 3});
        Files.write(historyPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));

        DeleteItemsResult result = service.deleteItems("rgb", "history", List.of("rgb_000001.png"));

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertFalse(Files.exists(historyDir.resolve("rgb_000001.png")));
        assertFalse(Files.exists(historyPolyDir.resolve("rgbP_000001.txt")));
    }

    @Test
    void deleteAllItemsShouldClearCollectFilesAndPoly() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path collectDir = tempDir.resolve("rgb").resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Files.createDirectories(collectPolyDir);
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(collectDir.resolve("rgb_000002.jpg"), new byte[]{4, 5, 6});
        Files.write(collectPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));
        Files.write(collectPolyDir.resolve("rgbP_000002.txt"), List.of("3 3", "4 4"));

        DeleteItemsResult result = service.deleteAllItems("rgb", "collect");

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertFalse(Files.exists(collectDir.resolve("rgb_000001.jpg")));
        assertFalse(Files.exists(collectDir.resolve("rgb_000002.jpg")));
        assertFalse(Files.exists(collectPolyDir.resolve("rgbP_000001.txt")));
        assertFalse(Files.exists(collectPolyDir.resolve("rgbP_000002.txt")));
    }

    @Test
    void deleteAllItemsShouldClearHistoryFilesAndPoly() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path historyDir = tempDir.resolve("rgb").resolve("history");
        Path historyPolyDir = historyDir.resolve("poly");
        Files.createDirectories(historyPolyDir);
        Files.write(historyDir.resolve("rgb_000001.png"), new byte[]{1, 2, 3});
        Files.write(historyDir.resolve("rgb_000002.jpg"), new byte[]{4, 5, 6});
        Files.write(historyPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));
        Files.write(historyPolyDir.resolve("rgbP_000002.txt"), List.of("3 3", "4 4"));

        DeleteItemsResult result = service.deleteAllItems("rgb", "history");

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertFalse(Files.exists(historyDir.resolve("rgb_000001.png")));
        assertFalse(Files.exists(historyDir.resolve("rgb_000002.jpg")));
        assertFalse(Files.exists(historyPolyDir.resolve("rgbP_000001.txt")));
        assertFalse(Files.exists(historyPolyDir.resolve("rgbP_000002.txt")));
    }

    @Test
    void deleteAllItemsShouldReturnEmptyResultForEmptyDirectory() {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());

        DeleteItemsResult result = service.deleteAllItems("rgb", "collect");

        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertTrue(result.getFailedNames().isEmpty());
    }

    @Test
    void deleteItemsShouldRejectIllegalTask() {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());

        BaseException exception = assertThrows(BaseException.class,
                () -> service.deleteItems("rgb", "invalid", List.of("rgb_000001.jpg")));

        assertEquals("Unsupported delete task: invalid", exception.getMessage());
    }

    @Test
    void deleteItemsShouldRejectPathTraversal() {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());

        BaseException exception = assertThrows(BaseException.class,
                () -> service.deleteItems("rgb", "collect", List.of("..\\secret.txt")));

        assertEquals("Illegal file name: ..\\secret.txt", exception.getMessage());
    }

    @Test
    void deleteAllItemsShouldRejectIllegalTask() {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());

        BaseException exception = assertThrows(BaseException.class,
                () -> service.deleteAllItems("rgb", "invalid"));

        assertEquals("Unsupported delete task: invalid", exception.getMessage());
    }

    @Test
    void getHistoryPageShouldReadOriginalImages() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path historyDir = tempDir.resolve("rgb").resolve("history");
        Path historyPolyDir = historyDir.resolve("poly");
        Files.createDirectories(historyPolyDir);
        Files.write(historyDir.resolve("rgb_000001.png"), new byte[]{1, 2, 3});
        Files.write(historyDir.resolve("rgb_000002.jpg"), new byte[]{4, 5, 6});
        Files.write(historyPolyDir.resolve("rgbP_000001.txt"), List.of("1 1", "2 2"));
        Files.write(historyPolyDir.resolve("rgbP_000002.txt"), List.of("3 3", "4 4"));

        assertEquals(2, service.getHistoryPage("rgb", 1, 5).getTotal());
        assertEquals(2, service.getHistoryPage("rgb", 1, 5).getFileList().size());
    }

    @Test
    void collectImageShouldReuseSharedPolyFromHistory() throws Exception {
        DataManagerServiceImpl service = new DataManagerServiceImpl(buildConfig());
        Path collectDir = tempDir.resolve("rgb").resolve("collect");
        Path collectPolyDir = collectDir.resolve("poly");
        Path historyPolyDir = tempDir.resolve("rgb").resolve("history").resolve("poly");
        Files.createDirectories(collectPolyDir);
        Files.createDirectories(historyPolyDir);
        Files.write(collectDir.resolve("rgb_000001.jpg"), new byte[]{1, 2, 3});
        Files.write(historyPolyDir.resolve("rgbP_000001_1712723456789.txt"), List.of("1 1", "2 2"));

        List<FileData> files = service.getFileList("rgb", "collect");

        assertEquals(1, files.size());
        assertEquals(2, files.get(0).getPoly().size());
    }

    private DataConfig buildConfig() {
        DataConfig config = new DataConfig();
        config.setBasePath(tempDir.toString());
        config.setCollect("collect");
        config.setProcess("process");
        config.setHistory("history");
        config.setWebPath(":8088/v2/static/res");
        return config;
    }
}
