package dym.filestorage.api.common;

public enum FileStatus {
    /**
     * File is being uploaded or processed.
     */
    PENDING,
    /**
     * File has been successfully uploaded and is available.
     */
    ACTIVE,
    /**
     * File has been deleted or is no longer available.
     */
    DELETED,
    /**
     * File upload failed due to an error.
     */
    FAILED
}
