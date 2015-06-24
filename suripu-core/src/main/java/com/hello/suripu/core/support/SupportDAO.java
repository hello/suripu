package com.hello.suripu.core.support;

import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

public interface SupportDAO {

    @RegisterMapper(SupportTopicMapper.class)
    @SqlQuery("SELECT * FROM support_topics WHERE enabled = true order by sort_order ASC")
    public List<SupportTopic> getTopics();
}
