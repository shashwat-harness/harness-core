package io.harness.ngpipeline.pipeline.repository;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class PipelineRepositoryImpl implements CustomPipelineRepository {
  MongoTemplate mongoTemplate;

  @Override
  public Page<NgPipelineEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<NgPipelineEntity> pipelineEntities = mongoTemplate.find(query, NgPipelineEntity.class);
    return PageableExecutionUtils.getPage(pipelineEntities, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), NgPipelineEntity.class));
  }

  @Override
  public List<NgPipelineEntity> findAllWithCriteria(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, NgPipelineEntity.class);
  }

  @Override
  public List<NgPipelineEntity> findAllWithCriteriaAndProjectOnFields(
      Criteria criteria, @NotNull List<String> includedFields, @NotNull List<String> excludedFields) {
    Query query = new Query(criteria);
    for (String field : includedFields) {
      query.fields().include(field);
    }
    for (String field : excludedFields) {
      query.fields().exclude(field);
    }
    return mongoTemplate.find(query, NgPipelineEntity.class);
  }
}
