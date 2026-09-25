package com.azv.service;

import com.azv.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
public class ResumeService {

    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final String FILE_NAME = "resume.pdf";
    private static final String PREVIEW_URL = "/api/resume/file";
    private static final String DOWNLOAD_URL = "/api/resume/file?download=true";
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final Path storageDirectory;
    private final Path resumeFile;

    public ResumeService(@Value("${resume.storage-dir:./data/resume}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
        this.resumeFile = this.storageDirectory.resolve(FILE_NAME).normalize();
        if (!this.resumeFile.startsWith(this.storageDirectory)) {
            throw new IllegalArgumentException("Invalid resume storage path");
        }
    }

    public ResumeMetadata getMetadata() {
        return findDocument()
                .map(this::toMetadata)
                .orElseGet(() -> new ResumeMetadata(
                        false, FILE_NAME, 0, null, PREVIEW_URL, DOWNLOAD_URL));
    }

    public Optional<ResumeDocument> findDocument() {
        try {
            if (!Files.isRegularFile(resumeFile, LinkOption.NOFOLLOW_LINKS)) {
                return Optional.empty();
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    resumeFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                return Optional.empty();
            }
            return Optional.of(new ResumeDocument(
                    resumeFile,
                    attributes.size(),
                    attributes.lastModifiedTime().toInstant()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read resume metadata", e);
        }
    }

    /**
     * Writes the upload beside the live file, validates it, then atomically swaps it in.
     * The live resume is therefore never partially written, even if validation or I/O fails.
     */
    public synchronized ResumeMetadata replace(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择 PDF 简历文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("简历 PDF 不能超过20MB");
        }

        Path temporaryFile = null;
        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(storageDirectory, ".resume-", ".tmp");
            copyWithLimit(file, temporaryFile);

            if (!hasPdfSignature(temporaryFile)) {
                throw new BizException("文件内容不是有效的 PDF");
            }

            try {
                Files.move(
                        temporaryFile,
                        resumeFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new BizException("当前存储不支持安全替换简历，请联系管理员");
            }
            temporaryFile = null;
            return getMetadata();
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to replace resume PDF", e);
            throw new BizException("简历保存失败，请重试");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary resume file: {}", temporaryFile, e);
                }
            }
        }
    }

    private void copyWithLimit(MultipartFile source, Path destination) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = source.getInputStream();
             OutputStream output = Files.newOutputStream(
                     destination, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE) {
                    throw new BizException("简历 PDF 不能超过20MB");
                }
                output.write(buffer, 0, read);
            }
        }
        if (total == 0) {
            throw new BizException("请选择 PDF 简历文件");
        }
    }

    private boolean hasPdfSignature(Path file) throws IOException {
        byte[] header = new byte[PDF_SIGNATURE.length];
        try (InputStream input = Files.newInputStream(file)) {
            if (input.readNBytes(header, 0, header.length) != header.length) {
                return false;
            }
        }
        return Arrays.equals(header, PDF_SIGNATURE);
    }

    private ResumeMetadata toMetadata(ResumeDocument document) {
        return new ResumeMetadata(
                true,
                FILE_NAME,
                document.size(),
                document.updatedAt(),
                PREVIEW_URL,
                DOWNLOAD_URL);
    }

    public record ResumeDocument(Path path, long size, Instant updatedAt) {
        public String etag() {
            return "\"" + Long.toHexString(size) + '-' + Long.toHexString(updatedAt.toEpochMilli()) + "\"";
        }
    }
}
