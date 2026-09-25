package com.azv.storage;

import com.azv.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class LocalFileStorageImpl implements FileStorageService {

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public StoredImage store(MultipartFile file) throws Exception {
        // ① 大小限制
        if (file.getSize() > MAX_SIZE) throw new BizException("图片不能超过10MB");

        byte[] bytes = file.getBytes();

        // ② 魔数校验：读文件头字节，不信任扩展名/content-type（都可伪造）
        String ext = detectExt(bytes);
        if (ext == null) throw new BizException("仅支持 JPG/PNG/WebP 图片");

        // ③ 按月分目录 + UUID 随机文件名（防路径穿越、防文件名冲突）
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve(month).normalize();
        if (!dir.startsWith(root)) throw new BizException("上传目录配置无效");
        Files.createDirectories(dir);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Files.write(dir.resolve(name), bytes);

        // ④ 生成缩略图（后台审核预览，300px 宽）
        String thumbName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        String thumbUrl = null;
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img != null) {
            int w = img.getWidth(), h = img.getHeight();
            int tw = Math.min(300, w);
            int th = Math.max(1, (int) ((double) tw / w * h));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = thumb.createGraphics();
            try {
                graphics.drawImage(
                        img.getScaledInstance(tw, th, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(thumb, "jpg", bos);          // 统一转 jpg 缩略图（webp 编码支持差）
            Files.write(dir.resolve(thumbName), bos.toByteArray());
            thumbUrl = "/uploads/" + month + "/" + thumbName;
        }

        // ⑤ 返回相对 URL（nginx 将来直接静态服务 /uploads/**）
        return new StoredImage("/uploads/" + month + "/" + name, thumbUrl);
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
