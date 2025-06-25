package dym.filestorage.api.helper;

import dym.filestorage.api.persistance.entity.FileMetadata;

public class FileHelper {

    private static final String PATH_DELIMETER = "/";

    public static String fileKeyFrom(FileMetadata fileMetadata) {
        return fileMetadata.getUserId() + PATH_DELIMETER + fileMetadata.getInStorageId();
    }
}
