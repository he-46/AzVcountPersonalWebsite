package com.azv.storage;

import com.azv.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageImplTest {
    @TempDir Path directory;

    @Test
    void storesValidImageAndThumbnail() throws Exception {
        LocalFileStorageImpl storage = storage();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);

        StoredImage saved = storage.store(new MockMultipartFile("file", "image.png", "image/png", out.toByteArray()));

        assertTrue(Files.exists(pathFor(saved.url())));
        assertTrue(Files.exists(pathFor(saved.thumbnailUrl())));
    }

    @Test
    void rejectsInvalidImageBeforeWritingFiles() throws Exception {
        LocalFileStorageImpl storage = storage();
        byte[] invalidPng = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2};

        assertThrows(BizException.class, () -> storage.store(
                new MockMultipartFile("file", "bad.png", "image/png", invalidPng)));
        try (Stream<Path> paths = Files.walk(directory)) {
            assertEquals(1, paths.count());
        }
    }

    private LocalFileStorageImpl storage() {
        LocalFileStorageImpl storage = new LocalFileStorageImpl();
        ReflectionTestUtils.setField(storage, "uploadDir", directory.toString());
        return storage;
    }

    private Path pathFor(String url) {
        return directory.resolve(url.substring("/uploads/".length()));
    }
}
