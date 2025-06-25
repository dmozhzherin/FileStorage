package dym.filestorage.api.dto;

import dym.filestorage.api.common.Visibility;
import dym.filestorage.api.exception.ApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Arrays;
import java.util.Set;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

public record UploadRequest(
        @NotBlank(message = "User ID cannot be blank.")
        String userId,
        @NotBlank(message = "File name cannot be blank.")
        @Pattern(regexp = "^(?!.*\\.\\.).*$",
                message = "File name contains invalid path sequence."
        )
        String fileName,
        String visibility,
        @Size(max = MAX_TAGS, message = "Number of tags cannot exceed {max}.")
        Set<String> tags
) {

    private static final int MAX_TAGS = 5;

    public Visibility getVisibility() {
        if (!hasText(visibility)) {
            return Visibility.PRIVATE; // Default visibility
        }
        try {
            return Visibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            var msg = format("Invalid visibility value: %s. Supported values: %s",
                    visibility, Arrays.toString(Visibility.values()));
            throw new ApiException(msg, e);
        }
    }

}
