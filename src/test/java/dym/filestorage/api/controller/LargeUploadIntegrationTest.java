package dym.filestorage.api.controller;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.MongoDBContainer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LargeUploadIntegrationTest {

    private static final String TEST_UPLOADS = "./target/test-uploads";

    private static final long GB2 = 2L * 1024 * 1024 * 1024;
    private static final long MB100 = 100L * 1024 * 1024;

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    @LocalServerPort
    private int port;

    private WebClient webClient;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("storage.local", () -> TEST_UPLOADS);
        registry.add("downloads.base-url", () -> "");
    }

    @BeforeAll
    public static void beforeAll() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void afterAll() {
        mongoDBContainer.stop();
    }

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @AfterEach
    void tearDown() throws IOException {
        deletePath(Path.of(TEST_UPLOADS));
    }

    private void deletePath(Path path) throws IOException {
        try (var files = Files.walk(path)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path1 -> {
                        try {
                            Files.deleteIfExists(path1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Test
    void testLargeFileUploads_shouldSucceed() {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/files")
                        .queryParam("fileName", "large1.dat")
                        .queryParam("userId", "test-user")
                        .queryParam("visibility", "private")
                        .build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new InputStreamResource(randomContentStream(GB2, System.currentTimeMillis()))))
                .exchangeToMono(response -> {
                    assertEquals(HttpStatus.CREATED, response.statusCode());
                    return Mono.empty();
                })
                .block();
    }

    @Test
    void testSimultaneousSameFileUploads_shouldFail() throws Exception {
        long commonSeed = 12345L;

        CompletableFuture<HttpStatusCode> upload1 = CompletableFuture.supplyAsync(() -> webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/files")
                        .queryParam("fileName", "sameFile1.dat")
                        .queryParam("userId", "test-user")
                        .queryParam("visibility", "PRIVATE")
                        .build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new InputStreamResource(randomContentStream(MB100, commonSeed))))
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block());

        CompletableFuture<HttpStatusCode> upload2 = CompletableFuture.supplyAsync(() -> webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/files")
                        .queryParam("fileName", "sameFile2.dat")
                        .queryParam("userId", "test-user")
                        .queryParam("visibility", "PUBLIC")
                        .build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new InputStreamResource(randomContentStream(MB100, commonSeed))))
                .exchangeToMono(response -> Mono.just(response.statusCode()))
                .block());

        CompletableFuture.allOf(upload1, upload2).join();

        assertNotEquals(upload1.get(), upload2.get(), "Both uploads should not succeed with the same content");
        assertThat(upload1.get() == HttpStatus.BAD_REQUEST || upload2.get() == HttpStatus.BAD_REQUEST).isTrue();
        assertThat(upload1.get() == HttpStatus.CREATED || upload2.get() == HttpStatus.CREATED).isTrue();
    }

    private InputStream randomContentStream(long size, long seed) {
        return new InputStream() {
            private long remaining = size;
            private final Random random = new Random(seed);
            private final byte[] buffer = new byte[65536];

            @Override
            public int read() {
                if (remaining <= 0) {
                    return -1;
                }
                int byteValue = random.nextInt(256);
                remaining--;
                return byteValue;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (remaining <= 0) {
                    return -1;
                }
                int bytesToRead = (int) Math.min(len, remaining);
                random.nextBytes(buffer);
                System.arraycopy(buffer, 0, b, off, bytesToRead);
                remaining -= bytesToRead;
                return bytesToRead;
            }
        };
    }
}