package dym.filestorage.api.dto;


import dym.filestorage.api.common.Visibility;
import dym.filestorage.api.exception.ApiException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class ListRequest {
    String userId;
    String tag;
    Visibility visibility;
    @Min(value = 0, message = "Page number must be 0 or greater")
    int page;
    @Positive(message = "Page size must be greater than 0")
    int size = 20;
    String sort = "uploadDate,desc";

    public Sort getSortBy() {
        String[] sortParts = sort.split(",");
        return switch (sortParts.length) {
            case 0 -> Sort.unsorted();
            case 1 -> Sort.by(Sort.Direction.ASC, sortParts[0]);
            case 2 -> Sort.by(Sort.Direction.fromString(sortParts[1]), sortParts[0]);
            default -> throw new ApiException("Sort parameter must be in the format 'field,direction(asc/desc)'");
        };
    }
}
