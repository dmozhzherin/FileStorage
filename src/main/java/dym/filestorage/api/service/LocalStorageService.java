package dym.filestorage.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class LocalStorageService implements FileStorageService {

    private static final int BUFFER_SIZE = 65536;

    private final Path fileStorageLocation;

    public LocalStorageService(@Value("${uploads.local}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    @Override
    public void saveFile(InputStream inputStream, String fileKey) throws IOException {
        var path = fileStorageLocation.resolve(fileKey);
        Files.createDirectories(path.getParent());

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        try (var outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            Files.deleteIfExists(path);
            throw new IOException("Could not store file with ID " + fileKey, e);
        }
    }

    @Override
    public InputStream loadFile(String fileKey) throws IOException {
        var path = fileStorageLocation.resolve(fileKey);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found " + path);
        }
        return Files.newInputStream(path);
    }

    @Override
    public void deleteFile(String fileKey) throws IOException {
        var path = fileStorageLocation.resolve(fileKey);
        if (!Files.exists(path)) {
            return;
        }
        Files.delete(path);
    }
}