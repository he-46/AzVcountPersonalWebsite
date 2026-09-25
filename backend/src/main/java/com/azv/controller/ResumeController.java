package com.azv.controller;

import com.azv.common.R;
import com.azv.service.ResumeMetadata;
import com.azv.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<R<ResumeMetadata>> metadata() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(R.ok(resumeService.getMetadata()));
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> file(
            @RequestParam(defaultValue = "false") boolean download,
            WebRequest request) {
        Optional<ResumeService.ResumeDocument> found = resumeService.findDocument();
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        ResumeService.ResumeDocument document = found.get();
        if (request.checkNotModified(document.etag(), document.updatedAt().toEpochMilli())) {
            return null;
        }

        ContentDisposition disposition = (download
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename("resume.pdf", StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(disposition);
        headers.setContentLength(document.size());
        headers.setCacheControl("public, max-age=0, must-revalidate");
        headers.setETag(document.etag());
        headers.setLastModified(document.updatedAt().toEpochMilli());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "frame-ancestors 'self'");

        return new ResponseEntity<>(new FileSystemResource(document.path()), headers, HttpStatus.OK);
    }
}
