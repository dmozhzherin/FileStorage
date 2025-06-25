package dym.filestorage.api.service;

import dym.filestorage.api.common.FileStatus;
import dym.filestorage.api.common.Visibility;
import dym.filestorage.api.dto.ListRequest;
import dym.filestorage.api.dto.UploadRequest;
import dym.filestorage.api.exception.ApiException;
import dym.filestorage.api.io.HashingStreamWrapper;
import dym.filestorage.api.persistance.entity.FileMetadata;
import dym.filestorage.api.persistance.repository.CustomMetadataRepository;
import dym.filestorage.api.persistance.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dym.filestorage.api.helper.FileHelper.fileKeyFrom;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final CustomMetadataRepository customMetadataRepository;
    private final FileStorageService fileStorageService;

    public FileMetadata uploadFile(InputStream inputStream,
                                   String contentType,
                                   UploadRequest uploadRequest) {

        FileMetadata metadata = metadataFrom(uploadRequest)
                .setContentType(contentType)
                .setStatus(FileStatus.PENDING);

        String storagePath = fileKeyFrom(metadata);

        try {
            fileMetadataRepository.save(metadata);

            // The underlying stream will be closed by the try-with-resources block in the controller upstream
            HashingStreamWrapper hashingWrapper = new HashingStreamWrapper(inputStream);
            fileStorageService.saveFile(hashingWrapper, storagePath);

            metadata.setHash(HexFormat.of().formatHex(hashingWrapper.getHash()));
            metadata.setSize(hashingWrapper.getBytesRead());
        } catch (DuplicateKeyException e) {
            throw new ApiException("File already exists: " + metadata.getFileName());
        } catch (IOException e) {
            fileMetadataRepository.save(metadata.setStatus(FileStatus.FAILED));
            log.error("Failed to store file: {} for user: {}", metadata.getInStorageId(), metadata.getUserId(), e);
            throw new ApiException("Failed to store the file.", e);
        }

        try {
            // Try to update the hash and hope it will be unique
            fileMetadataRepository.save(metadata.setStatus(FileStatus.ACTIVE));
        } catch (DuplicateKeyException e) {
            //It's still PENDING
            fileMetadataRepository.save(metadata.setStatus(FileStatus.FAILED));
            try {
                fileStorageService.deleteFile(storagePath);
            } catch (IOException ex) {
                log.error("Failed to clean up failed upload: {} for user: {}", metadata.getInStorageId(), metadata.getUserId(), ex);
            }

            throw new ApiException("File with the same content already exists for user " + metadata.getUserId());
        }

        //In a better world this could be done asynchronously after returning the response
        updateContentType(metadata);

        return metadata;
    }

    private void updateContentType(FileMetadata fileMetadata) {
        if (fileMetadata.getContentType() == null
                || APPLICATION_OCTET_STREAM.isCompatibleWith(MimeType.valueOf(fileMetadata.getContentType()))) {

            try (TikaInputStream is = TikaInputStream.get(fileStorageService.loadFile(fileKeyFrom(fileMetadata)))) {
                Metadata tikaMetadata = new Metadata();
                tikaMetadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileMetadata.getFileName());
                TikaConfig tika = new TikaConfig();
                org.apache.tika.mime.MediaType mediaType = tika.getDetector().detect(is, tikaMetadata);

                if (!mediaType.getType().equalsIgnoreCase(fileMetadata.getContentType())) {
                    fileMetadata.setContentType(mediaType.toString());
                    fileMetadataRepository.save(fileMetadata);
                }

            } catch (TikaException | IOException e) {
                log.error("Failed to determine content type for file: {} for user: {}",
                        fileMetadata.getInStorageId(), fileMetadata.getUserId(), e);
            }
        }
    }

    private FileMetadata metadataFrom(UploadRequest uploadRequest) {
        return new FileMetadata()
                .setFileName(uploadRequest.fileName())
                .setUserId(uploadRequest.userId())
                .setVisibility(uploadRequest.getVisibility())
                .setTags(uploadRequest.tags())
                .setUploadDate(Clock.systemUTC().millis())
                .setInStorageId(UUID.randomUUID().toString());
    }

    public List<FileMetadata> listUserFiles(ListRequest listRequest) {
        Pageable pageable = PageRequest.of(listRequest.getPage(), listRequest.getSize(), listRequest.getSortBy());
        return customMetadataRepository.findByUser(
                listRequest.getUserId(),
                listRequest.getVisibility(),
                listRequest.getTag(),
                pageable);
    }

    public List<FileMetadata> listPublicFiles(ListRequest listRequest) {
        Pageable pageable = PageRequest.of(listRequest.getPage(), listRequest.getSize(), listRequest.getSortBy());
        return customMetadataRepository.findPublic(listRequest.getTag(), pageable);
    }

    public List<String> getAccessibleTags(String userId) {
        return fileMetadataRepository.findAccessibleTags(userId);
    }

    public Pair<FileMetadata, InputStream> getFile(String inStorageId, String userId) throws IOException {
        FileMetadata metadata = getFileMetadata(inStorageId, userId);

        return Pair.of(metadata, fileStorageService.loadFile(fileKeyFrom(metadata)));
    }

    public void deleteFile(String inStorageId, String userId) throws IOException {
        FileMetadata metadata = getFileMetadata(inStorageId, userId);

        fileStorageService.deleteFile(fileKeyFrom(metadata));

        fileMetadataRepository.save(metadata.setStatus(FileStatus.DELETED));
    }

    private FileMetadata getFileMetadata(String inStorageId, String userId) throws FileNotFoundException {
        Optional<FileMetadata> metadataOptional = fileMetadataRepository.findActiveByStorageId(inStorageId);

        FileMetadata metadata = metadataOptional.orElseThrow(
                () -> new FileNotFoundException("File not found: " + inStorageId)
        );

        if (metadata.getVisibility() == Visibility.PRIVATE && !metadata.getUserId().equals(userId)) {
            //This is for internal use. External users should get "File not found"
            throw new SecurityException("Access denied: " + inStorageId);
        }
        return metadata;
    }


}
