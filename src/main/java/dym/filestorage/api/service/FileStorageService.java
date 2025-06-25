package dym.filestorage.api.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    void saveFile(InputStream inputStream, String fileKey) throws IOException;

    InputStream loadFile(String fileKey) throws IOException;

    void deleteFile(String fileKey) throws IOException;
}