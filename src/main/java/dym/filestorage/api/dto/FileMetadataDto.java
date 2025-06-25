package dym.filestorage.api.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import dym.filestorage.api.common.Visibility;
import dym.filestorage.api.persistance.entity.FileMetadata;

import java.time.Instant;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileMetadataDto(
        String fileName,
        String contentType,
        String size,
        Visibility visibility,
        Set<String> tags,
        String link,
        //UTC DateTime, correct conversion to the user's timezone is not covered in the scope of this task
        Instant uploadDate
) {

    public static FileMetadataDto from(FileMetadata fileMetadata) {
        return new FileMetadataDto(
                fileMetadata.getFileName(),
                fileMetadata.getContentType(),
                String.valueOf(fileMetadata.getSize()),
                fileMetadata.getVisibility(),
                fileMetadata.getTags(),
                fileMetadata.getInStorageId(),
                Instant.ofEpochMilli(fileMetadata.getUploadDate())
        );

    }
}
