package dym.filestorage.api.persistance.entity;

import dym.filestorage.api.common.FileStatus;
import dym.filestorage.api.common.Visibility;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Accessors(chain = true)
@Document(collection = "files")
@CompoundIndex(name = "user_filename_idx",
        def = "{'userId': 1, 'fileName': 1}",
        partialFilter = "{'status': { $in: ['PENDING', 'ACTIVE'] }}",
        unique = true)
@CompoundIndex(name = "user_sha256_hash_idx",
        def = "{'userId': 1, 'hash': 1}",
        partialFilter = "{'status': { $eq: 'ACTIVE' }}",
        unique = true)

public class FileMetadata {

    @Id
    private String id;

    private String fileName;
    private String userId;
    private Visibility visibility;
    private Set<String> tags;
    private long uploadDate;
    private String contentType;
    private long size;
    private String hash;
    private FileStatus status;

    @Indexed(name = "inStorageId_idx", unique = true, background = true,
            partialFilter = "{'status': { $eq: 'ACTIVE' }}")
    private String inStorageId;
}
