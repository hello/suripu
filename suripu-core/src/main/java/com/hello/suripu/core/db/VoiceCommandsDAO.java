package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.VoiceCommandMapper;
import com.hello.suripu.core.models.VoiceCommand;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

/**
 * Created by david on 2/3/17.
 */
@RegisterMapper(VoiceCommandMapper.class)
public  interface VoiceCommandsDAO {


    @SqlQuery("select vc.command, vcs.command_title, vct.title, vct.description, vct.icon from voice_commands vc " +
            "left join voice_command_subtopics vcs on vcs.id = vc.voice_command_subtopic_id " +
            "left join voice_command_topics vct on vct.id = vcs.voice_command_topic_id")
    List<VoiceCommand> getCommands();

}