package com.azv.storage;

import com.azv.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Iterator;

@Component
public class LocalFileStorageImpl implements FileStorageService {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int MAX_DIMENSION = 12_000;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public StoredImage store(MultipartFile file) throws Exception {
        // ① 大小限制
        if (file.getSize() > MAX_SIZE) throw new BizException("图片不能超过10MB");

        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_SIZE) throw new BizException("图片不能超过10MB");

        // ② 魔数校验：读文件头字节，不信任扩展名/content-type（都可伪造）
        String ext = detectExt(bytes);
        if (ext == null) throw new BizException("仅支持 JPG/PNG/WebP 图片");

        // ③ 先读取尺寸再解码，避免小文件声明极大尺寸导致内存耗尽。
        // JDK 默认没有 WebP 解码器：WebP 保留原图，仍受 10 MB 文件大小限制。
        BufferedImage img = decodeChecked(bytes, ext);
        byte[] thumbnail = thumbnailBytes(img);

        // ④ 按月分目录 + UUID 随机文件名（防路径穿越、防文件名冲突）
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve(month).normalize();
        if (!dir.startsWith(root)) throw new BizException("上传目录配置无效");
        Files.createDirectories(dir);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path originalPath = dir.resolve(name);
        Path thumbnailPath = thumbnail == null ? null : dir.resolve(UUID.randomUUID().toString().replace("-", "") + ".jpg");
        try {
            Files.write(originalPath, bytes);
            if (thumbnailPath != null) Files.write(thumbnailPath, thumbnail);
        } catch (Exception e) {
            try { Files.deleteIfExists(originalPath); } catch (Exception cleanupError) { e.addSuppressed(cleanupError); }
            if (thumbnailPath != null) {
                try { Files.deleteIfExists(thumbnailPath); } catch (Exception cleanupError) { e.addSuppressed(cleanupError); }
            }
            throw e;
        }

        String thumbUrl = thumbnailPath == null ? null : "/uploads/" + month + "/" + thumbnailPath.getFileName();
        // ⑤ 返回相对 URL（nginx 将来直接静态服务 /uploads/**）
        return new StoredImage("/uploads/" + month + "/" + name, thumbUrl);
    }

    private BufferedImage decodeChecked(byte[] bytes, String ext) throws Exception {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new BizException("图片内容无效");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                if ("webp".equals(ext)) return null;
                throw new BizException("图片内容无效");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new BizException("图片像素尺寸过大");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new BizException("图片内容无效");
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BizException("图片内容无效");
        }
    }

    private byte[] thumbnailBytes(BufferedImage img) throws Exception {
        if (img == null) return null;
        int tw = Math.min(300, img.getWidth());
        int th = Math.max(1, (int) ((double) tw / img.getWidth() * img.getHeight()));
        BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumb.createGraphics();
        try {
            graphics.drawImage(img.getScaledInstance(tw, th, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(thumb, "jpg", output)) throw new BizException("图片缩略图生成失败");
        return output.toByteArray();
    }

    @Override
    public void delete(String url) {
        try {
            if (url == null || !url.startsWith("/uploads/")) return;
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path p = root.resolve(url.substring("/uploads/".length())).normalize();
            if (!p.startsWith(root)) return;
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
            // 删除失败不阻塞业务（文件残留可接受）
        }
    }

    /** 魔数检测：返回 jpg/png/webp，不认识返回 null */
    private String detectExt(byte[] b) {
        // JPEG: FF D8 FF
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        // WebP: RIFF....WEBP
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
