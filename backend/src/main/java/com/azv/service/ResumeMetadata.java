package com.azv.service;

import java.time.Instant;

/** Public metadata for the currently published resume. */
public record ResumeMetadata(
        boolean available,
        String fileName,
        long size,
        Instant updatedAt,
        String previewUrl,
        String downloadUrl
) {
}
