package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagePreviewServiceTest {

    private final ImagePreviewService imagePreviewService = new ImagePreviewService();

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateCompressedPreviewAsJpg() throws Exception {
        File source = tempDir.resolve("rgb_000001.png").toFile();
        BufferedImage image = new BufferedImage(2400, 1600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLUE);
            for (int x = 0; x < image.getWidth(); x += 40) {
                graphics.drawLine(x, 0, image.getWidth() - x - 1, image.getHeight() - 1);
            }
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", source);

        DataConfig.PreviewItem previewItem = new DataConfig.PreviewItem();
        previewItem.setLongEdge(1280);
        previewItem.setTargetSizeKb(120);
        previewItem.setMinQuality(0.6d);

        File preview = tempDir.resolve("rgb_000001.jpg").toFile();
        imagePreviewService.generatePreview(source, preview, previewItem);

        assertTrue(preview.exists());
        assertTrue(preview.length() > 0);
        assertTrue(preview.length() <= 160 * 1024L);
        BufferedImage previewImage = ImageIO.read(preview);
        assertTrue(Math.max(previewImage.getWidth(), previewImage.getHeight()) <= 1280);
    }
}
