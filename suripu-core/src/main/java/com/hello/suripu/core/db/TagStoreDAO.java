package com.hello.suripu.core.db;

import com.google.common.base.Optional;

import com.hello.suripu.core.models.Tag;

import java.util.List;

public interface TagStoreDAO {


    void createTag(Tag tag, TagStoreDAODynamoDB.Type type);

    Optional<Tag> getTag(String teamName, TagStoreDAODynamoDB.Type type);

    List<Tag> getTags(TagStoreDAODynamoDB.Type type);

    void add(String tagName, TagStoreDAODynamoDB.Type type, List<String> ids);

    void remove(String tagName, TagStoreDAODynamoDB.Type type, List<String> ids);

    void delete(Tag tag, TagStoreDAODynamoDB.Type type);
}
