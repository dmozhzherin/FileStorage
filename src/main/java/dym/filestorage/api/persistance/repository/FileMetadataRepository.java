package dym.filestorage.api.persistance.repository;

import dym.filestorage.api.persistance.entity.FileMetadata;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'userId': ?0 }, { 'visibility': 'PUBLIC' } ] } }",
            "{ $match: { 'status': 'ACTIVE' } }",
            "{ $unwind: '$tags' }",
            "{ $group: { _id: { $toLower: '$tags' } } }",
            "{ $project: { _id: 1 } }",
            "{ $sort: { '_id': 1 } }",
    })
    List<String> findAccessibleTags(String userId);

    @Query("{ 'inStorageId': ?0, 'status': 'ACTIVE' }")
    Optional<FileMetadata> findActiveByStorageId(String inStorageId);

}