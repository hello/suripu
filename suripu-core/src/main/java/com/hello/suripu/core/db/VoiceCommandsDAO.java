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


    @SqlQuery("SELECT * FROM voice_commands")
    List<VoiceCommand> getCommands();

}