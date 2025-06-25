package dym.filestorage.api.dto;

import dym.filestorage.api.common.Visibility;

import java.time.Instant;

public record UploadResponse(
        String fileName,
        String contentType,
        Visibility visibility,
        long size,
        Instant timestamp,
        String url
) {
}
