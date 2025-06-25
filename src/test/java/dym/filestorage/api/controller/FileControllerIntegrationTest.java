package dym.filestorage.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dym.filestorage.api.dto.UploadRequest;
import dym.filestorage.api.persistance.entity.FileMetadata;
import dym.filestorage.api.persistance.repository.FileMetadataRepository;
import dym.filestorage.api.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;

import java.util.Set;

import static dym.filestorage.api.common.Visibility.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerIntegrationTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        mongoDBContainer.start();
    }

    @AfterEach
    void tearDown() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    void uploadFileStream_shouldStoreMetadataInDatabase_andReturnCreatedResponse() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContent("Test content".getBytes());
        mockHttpServletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

        mockMvc.perform(post("/files")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("some content")
                        .param("userId", "tester")
                        .param("fileName", "testFile.txt")
                        .param("tags", "tag1,tag2")
                        .requestAttr(MockHttpServletRequest.class.getName(), mockHttpServletRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("testFile.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        var storedMetadata = fileMetadataRepository.findAll();
        assertThat(storedMetadata).hasSize(1);
        FileMetadata metadata = storedMetadata.getFirst();
        assertThat(metadata.getFileName()).isEqualTo("testFile.txt");
        assertThat(metadata.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(metadata.getSize()).isEqualTo(12);
        assertThat(metadata.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(metadata.getVisibility()).isEqualTo(PRIVATE);    //default visibility
    }

    @Test
    void uploadFileStream_shouldReturnBadRequest_whenInvalidRequest() throws Exception {
        mockMvc.perform(post("/files")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content("some content")
                .param("userId", "12345")
                .param("fileName", "invalid file..name")
        ).andExpect(status().isBadRequest());

        assertThat(fileMetadataRepository.findAll()).isEmpty();
    }

    @Test
    void uploadFileStream_shouldReturnInternalServerError_whenUnexpectedErrorOccurs() throws Exception {
        UploadRequest uploadRequest = new UploadRequest(
                "validUserId",
                "testFile.txt",
                "PUBLIC",
                Set.of("tag1", "tag2")
        );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContent("Test content".getBytes());
        mockHttpServletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
        mockHttpServletRequest.setAttribute(FileService.class.getName(), null);

        mockMvc.perform(post("/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadRequest))
                        .requestAttr(MockHttpServletRequest.class.getName(), mockHttpServletRequest))
                .andExpect(status().isBadRequest());

        assertThat(fileMetadataRepository.findAll()).isEmpty();
    }
}