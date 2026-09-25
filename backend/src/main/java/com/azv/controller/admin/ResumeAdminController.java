package com.azv.controller.admin;

import com.azv.common.R;
import com.azv.service.ResumeMetadata;
import com.azv.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/resume")
@RequiredArgsConstructor
public class ResumeAdminController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<R<ResumeMetadata>> replace(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(R.ok(resumeService.replace(file)));
    }
}
