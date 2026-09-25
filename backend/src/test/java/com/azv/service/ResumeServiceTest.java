package com.azv.service;

import com.azv.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void replacesResumeWithValidPdf() throws Exception {
        ResumeService service = new ResumeService(temporaryDirectory.toString());
        byte[] pdf = "%PDF-1.7\nvalid test document".getBytes();
        MockMultipartFile upload = new MockMultipartFile(
                "file", "my-resume.pdf", "application/pdf", pdf);

        ResumeMetadata metadata = service.replace(upload);

        assertTrue(metadata.available());
        assertEquals("resume.pdf", metadata.fileName());
        assertEquals(pdf.length, metadata.size());
        assertEquals("/api/resume/file", metadata.previewUrl());
        assertEquals("/api/resume/file?download=true", metadata.downloadUrl());
        assertArrayEquals(pdf, Files.readAllBytes(temporaryDirectory.resolve("resume.pdf")));
    }

    @Test
    void invalidUploadDoesNotDamageExistingResume() throws Exception {
        ResumeService service = new ResumeService(temporaryDirectory.toString());
        byte[] original = "%PDF-1.7\noriginal resume".getBytes();
        service.replace(new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", original));

        MockMultipartFile invalid = new MockMultipartFile(
                "file", "replacement.pdf", "application/pdf", "not a pdf".getBytes());

        BizException error = assertThrows(BizException.class, () -> service.replace(invalid));
        assertEquals("文件内容不是有效的 PDF", error.getMessage());
        assertArrayEquals(original, Files.readAllBytes(temporaryDirectory.resolve("resume.pdf")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".resume-")));
        }
    }

    @Test
    void rejectsAdvertisedFileLargerThanLimitBeforeReadingIt() throws Exception {
        ResumeService service = new ResumeService(temporaryDirectory.toString());
        MultipartFile upload = mock(MultipartFile.class);
        when(upload.isEmpty()).thenReturn(false);
        when(upload.getSize()).thenReturn(ResumeService.MAX_FILE_SIZE + 1);

        BizException error = assertThrows(BizException.class, () -> service.replace(upload));

        assertEquals("简历 PDF 不能超过20MB", error.getMessage());
        assertFalse(Files.exists(temporaryDirectory.resolve("resume.pdf")));
    }
}
