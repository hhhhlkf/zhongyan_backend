package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

@Service
@Slf4j
public class ImagePreviewService {

    public String buildPreviewFileName(String originalFileName) {
        int lastDot = originalFileName.lastIndexOf('.');
        String baseName = lastDot >= 0 ? originalFileName.substring(0, lastDot) : originalFileName;
        return baseName + ".jpg";
    }

    public File generatePreview(File sourceFile, File targetFile, DataConfig.PreviewItem config) {
        try {
            BufferedImage source = ImageIO.read(sourceFile);
            if (source == null) {
                throw new BaseException("Unsupported image file: " + sourceFile.getAbsolutePath());
            }
            Files.createDirectories(targetFile.toPath().getParent());

            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();
            int longEdgeLimit = normalizeInt(config == null ? null : config.getLongEdge(), 1280);
            int targetSizeKb = normalizeInt(config == null ? null : config.getTargetSizeKb(), 120);
            double minQuality = normalizeDouble(config == null ? null : config.getMinQuality(), 0.65d);

            double scale = calculateInitialScale(sourceWidth, sourceHeight, longEdgeLimit);
            byte[] bytes = null;

            for (int attempt = 0; attempt < 6; attempt++) {
                int scaledWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
                int scaledHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
                BufferedImage resized = resizeToJpegCompatible(source, scaledWidth, scaledHeight);
                bytes = encodeUnderTarget(resized, targetSizeKb, minQuality);
                if (bytes.length <= targetSizeKb * 1024L || scale <= 0.2d) {
                    break;
                }
                scale *= 0.85d;
            }

            if (bytes == null) {
                throw new BaseException("Failed to generate preview for " + sourceFile.getName());
            }
            Files.write(targetFile.toPath(), bytes);
            return targetFile;
        } catch (IOException e) {
            throw new BaseException("Failed to generate preview: " + sourceFile.getAbsolutePath(), e);
        }
    }

    public void trimPreviewDirectory(File imageDir, File polyDir, int retainCount) {
        if (retainCount <= 0 || !imageDir.exists() || !imageDir.isDirectory()) {
            return;
        }
        File[] images = imageDir.listFiles((dir, name) -> isImageFile(name));
        if (images == null || images.length <= retainCount) {
            return;
        }
        Arrays.sort(images, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = retainCount; i < images.length; i++) {
            File image = images[i];
            if (!image.delete()) {
                log.warn("Failed to delete preview image {}", image.getAbsolutePath());
            }
            if (polyDir != null && polyDir.exists()) {
                String polyName = com.gosling.bms.dao.entity.FileData.getPolyFileName(image.getName());
                File polyFile = new File(polyDir, polyName);
                if (polyFile.exists() && !polyFile.delete()) {
                    log.warn("Failed to delete preview poly {}", polyFile.getAbsolutePath());
                }
            }
        }
    }

    private byte[] encodeUnderTarget(BufferedImage image, int targetSizeKb, double minQuality) throws IOException {
        double quality = 0.92d;
        byte[] best = null;
        while (quality >= minQuality) {
            byte[] bytes = encodeJpeg(image, quality);
            best = bytes;
            if (bytes.length <= targetSizeKb * 1024L) {
                return bytes;
            }
            quality -= 0.08d;
        }
        return best == null ? encodeJpeg(image, minQuality) : best;
    }

    private byte[] encodeJpeg(BufferedImage image, double quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new BaseException("JPEG writer not available");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality((float) quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private BufferedImage resizeToJpegCompatible(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private double calculateInitialScale(int width, int height, int longEdgeLimit) {
        int longEdge = Math.max(width, height);
        if (longEdge <= longEdgeLimit) {
            return 1.0d;
        }
        return (double) longEdgeLimit / (double) longEdge;
    }

    private int normalizeInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private double normalizeDouble(Double value, double defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }
}
