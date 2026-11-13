package org.smorodin.infrastructure.avatar;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

final class AvatarImageProcessor {
    static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
    private AvatarImageProcessor() {}

    static PlayerAvatarService.AvatarData process(byte[] bytes, String type) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IllegalArgumentException("Invalid avatar image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid avatar image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(type.equals("image/png") && format.equals("png")
                        || type.equals("image/jpeg") && (format.equals("jpeg") || format.equals("jpg")))) {
                    throw new IllegalArgumentException("Image content does not match its type");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 12000 || height > 12000
                        || (long) width * height > 64_000_000L)
                    throw new IllegalArgumentException("Image dimensions are too large");

                ImageReadParam param = reader.getDefaultReadParam();
                int subsample = Math.max(1, (int) Math.ceil(Math.max(width, height) / 1024.0));
                param.setSourceSubsampling(subsample, subsample, 0, 0);
                BufferedImage image = reader.read(0, param);
                if (image == null) throw new IllegalArgumentException("Invalid avatar image");
                return encode(image);
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("Invalid avatar image", e); }
    }

    private static PlayerAvatarService.AvatarData encode(BufferedImage source) throws IOException {
        double scale = Math.min(1.0, 512.0 / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
        boolean alpha = source.getColorModel().hasAlpha();
        BufferedImage outputImage = new BufferedImage(w, h,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = outputImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 0, 0, w, h, null);
        } finally { g.dispose(); }

        String format = alpha ? "png" : "jpeg";
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) throw new IOException("Image encoder unavailable");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (!alpha && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.82f);
            }
            writer.write(null, new IIOImage(outputImage, null, null), param);
            stream.flush();
            byte[] encoded = out.toByteArray();
            if (encoded.length > 2 * 1024 * 1024)
                throw new IllegalArgumentException("Processed avatar exceeds 2 MB");
            return new PlayerAvatarService.AvatarData(encoded, alpha ? "image/png" : "image/jpeg");
        } finally { writer.dispose(); }
    }
}
