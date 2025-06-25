package dym.filestorage.api.controller;

import com.jayway.jsonpath.JsonPath;
import dym.filestorage.api.persistance.entity.FileMetadata;
import dym.filestorage.api.persistance.repository.FileMetadataRepository;
import org.junit.jupiter.api.AfterAll;
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

import static dym.filestorage.api.common.Visibility.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerIntegrationTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("uploads.local", () -> "./target/test-uploads");
        registry.add("downloads.base-url", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @BeforeAll
    public static void beforeAll() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void afterAll() {
        mongoDBContainer.stop();
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

        mockMvc.perform(post("/files")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content("some content")
                .param("fileName", "valid file.name")
        ).andExpect(status().isBadRequest());

        mockMvc.perform(post("/files")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content("some content")
                .param("userId", "validUserId")
                .param("fileName", "valid file.name")
                .param("visibility", "INVALID")
        ).andExpect(status().isBadRequest());

        mockMvc.perform(post("/files")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content("some content")
                .param("userId", "validUserId")
                .param("fileName", "valid file.name")
                .param("tags", "invalid,number,of,tags,exceeding,max")
        ).andExpect(status().isBadRequest());

        assertThat(fileMetadataRepository.findAll()).isEmpty();
    }

    @Test
    void downloadFile_shouldReturn404_whenWrongUser() throws Exception {
        var response = mockMvc.perform(post("/files")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("test content")
                        .param("userId", "owner")
                        .param("fileName", "private.txt"))
                .andExpect(status().isCreated())
                .andReturn();

        var fileId = JsonPath.read(response.getResponse().getContentAsString(), "$.url");

        mockMvc.perform(get("/files/{id}", fileId)
                        .param("userId", "other-user"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/files/{id}", fileId)
                        .param("userId", "owner"))
                .andExpect(status().isOk());
    }

    @Test
    void downloadFile_shouldReturnSuccess_whenPublicFile() throws Exception {
        var response = mockMvc.perform(post("/files")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("test content")
                        .param("userId", "owner")
                        .param("visibility", "public")
                        .param("fileName", "public.txt"))
                .andExpect(status().isCreated())
                .andReturn();

        var fileId = JsonPath.read(response.getResponse().getContentAsString(), "$.url");

        mockMvc.perform(get("/files/{id}", fileId)
                        .param("userId", "other user"))
                .andExpect(status().isOk())
                .andExpect(content().string("test content"));
    }
}