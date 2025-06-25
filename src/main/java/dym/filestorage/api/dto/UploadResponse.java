package dym.filestorage.api.dto;

import java.time.Instant;

public record UploadResponse(
        String fileName,
        String contentType,
        long size,
        Instant timestamp,
        String url
) {
}
