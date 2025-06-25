package dym.filestorage.api.controller;

import dym.filestorage.api.dto.*;
import dym.filestorage.api.persistance.entity.FileMetadata;
import dym.filestorage.api.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Valid
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    private @Value("${downloads.base-url}") String baseUrl;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadResponse> uploadFileStream(
            @Valid UploadRequest uploadRequest,
            HttpServletRequest httpRequest) throws URISyntaxException, IOException {

        try (InputStream inputStream = httpRequest.getInputStream()) {

            var contentType = httpRequest.getContentType();
            FileMetadata metadata = fileService.uploadFile(inputStream, contentType, uploadRequest);
            URI uri = new URI(baseUrl).resolve(metadata.getInStorageId());

            UploadResponse response = new UploadResponse(
                    metadata.getFileName(),
                    metadata.getContentType(),
                    metadata.getVisibility(),
                    metadata.getSize(),
                    Instant.ofEpochMilli(metadata.getUploadDate()),
                    uri.toString()
            );

            return ResponseEntity.created(uri).body(response);
        }
    }

    @GetMapping(path = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<FileMetadataDto> listPublicFiles(@Valid ListRequest listRequest) {
        return toPageResponse(fileService.listPublicFiles(listRequest), listRequest.getPage(), listRequest.getSize());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<FileMetadataDto> listUserFiles(@Valid ListRequest listRequest) {
        return toPageResponse(fileService.listUserFiles(listRequest), listRequest.getPage(), listRequest.getSize());
    }

    @GetMapping(path = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<String>> listTags(
            @NotBlank(message = "UserId required!")
            @RequestParam(value = "userId", required = false) String userId) {
        return Collections.singletonMap("tags", fileService.getAccessibleTags(userId));
    }

    @GetMapping("/{inStorageId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String inStorageId,
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            Pair<FileMetadata, InputStream> file = fileService.getFile(inStorageId, userId);

            FileMetadata metadata = file.getFirst();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(metadata.getFileName(), StandardCharsets.UTF_8) + "\"")
                    .lastModified(metadata.getUploadDate())
                    .contentLength(metadata.getSize())
                    .contentType(MediaType.parseMediaType(metadata.getContentType()))
                    .body(new InputStreamResource(file.getSecond()));
        } catch (FileNotFoundException | SecurityException e) {
            log.error("File download failed for user: " + userId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file.", e);
        }
    }

    @DeleteMapping("/{inStorageId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String inStorageId,
            @RequestParam("userId") String userId) {
        try {
            fileService.deleteFile(inStorageId, userId);
            return ResponseEntity.noContent().build();
        } catch (FileNotFoundException | SecurityException e) {
            log.error("File deletion failed for user: " + userId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file: " + e.getMessage(), e);
        }
    }

    private PageResponse<FileMetadataDto> toPageResponse(List<FileMetadata> data, int page, int size) {
        List<FileMetadataDto> responseData = data.stream()
                .map(FileMetadataDto::from)
                .toList();
        return new PageResponse<>(page, size, responseData);
    }

}