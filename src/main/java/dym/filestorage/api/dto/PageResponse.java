package dym.filestorage.api.dto;

import java.util.Collection;

public record PageResponse<T>(
        int page,
        int size,

        Collection<T> data
) {
}
