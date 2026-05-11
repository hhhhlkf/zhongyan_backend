package com.zhongyan.uav.asset.infrastructure.preview;

import com.zhongyan.uav.asset.port.AssetObject;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import com.zhongyan.uav.asset.port.PreviewGeneratorPort;
import com.zhongyan.uav.asset.port.PreviewRequest;
import com.zhongyan.uav.asset.port.PreviewResult;
import com.zhongyan.uav.asset.port.StoredObject;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Component
public class JavaImagePreviewGenerator implements PreviewGeneratorPort {
    private static final int DEFAULT_LONG_EDGE = 1280;
    private static final float DEFAULT_QUALITY = 0.82f;

    private final AssetStoragePort assetStoragePort;

    public JavaImagePreviewGenerator(AssetStoragePort assetStoragePort) {
        this.assetStoragePort = Objects.requireNonNull(assetStoragePort, "assetStoragePort must not be null");
    }

    @Override
    public PreviewResult generate(PreviewRequest request) {
        try {
            BufferedImage source;
            try (InputStream input = assetStoragePort.read(request.sourceObjectKey())) {
                source = ImageIO.read(input);
            }
            if (source == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "source object is not a supported image: " + request.sourceObjectKey());
            }
            BufferedImage preview = resize(source, intParameter(request.parameters(), "longEdge", DEFAULT_LONG_EDGE));
            byte[] encoded = encodeJpeg(preview, floatParameter(request.parameters(), "quality", DEFAULT_QUALITY));
            StoredObject stored = assetStoragePort.put(new AssetObject(
                    request.previewObjectKey(),
                    new ByteArrayInputStream(encoded),
                    encoded.length,
                    "image/jpeg",
                    Map.of("sourceObjectKey", request.sourceObjectKey(), "assetId", request.assetId())));
            return new PreviewResult(stored.objectKey(), stored.contentType(), stored.contentLength(),
                    "sha256:" + sha256(encoded));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to generate image preview", exception);
        }
    }

    private BufferedImage resize(BufferedImage source, int longEdge) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int maxEdge = Math.max(sourceWidth, sourceHeight);
        double scale = maxEdge <= longEdge ? 1d : (double) longEdge / maxEdge;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "jpeg writer is not available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private int intParameter(Map<String, Object> parameters, String name, int fallback) {
        Object value = parameters.get(name);
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return fallback;
    }

    private float floatParameter(Map<String, Object> parameters, String name, float fallback) {
        Object value = parameters.get(name);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return fallback;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "sha-256 digest is not available", exception);
        }
    }
}
