package dym.filestorage.api.persistance.repository;

import dym.filestorage.api.common.FileStatus;
import dym.filestorage.api.common.Visibility;
import dym.filestorage.api.persistance.entity.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class CustomMetadataRepository {

    private final MongoTemplate mongoTemplate;

    public List<FileMetadata> findByUser(String userId, Visibility visibility, String tag, Pageable pageable) {
        Criteria criteria = where("userId").is(userId).and("status").is(FileStatus.ACTIVE);

        if (visibility != null) {
            criteria.and("visibility").is(visibility);
        }

        if (hasText(tag)) {
            criteria.and("tags").is(tag);
        }

        Query query = query(criteria).with(pageable);

        return mongoTemplate.find(query, FileMetadata.class);
    }

    public List<FileMetadata> findPublic(String tag, Pageable pageable) {
        Criteria criteria = where("status").is(FileStatus.ACTIVE)
                .and("visibility").is(Visibility.PUBLIC);

        if (hasText(tag)) {
            criteria.and("tags").is(tag);
        }

        Query query = query(criteria).with(pageable);

        return mongoTemplate.find(query, FileMetadata.class);
    }
}
