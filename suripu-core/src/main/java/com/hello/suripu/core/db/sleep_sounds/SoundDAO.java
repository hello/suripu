package com.hello.suripu.core.db.sleep_sounds;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.mappers.sleep_sounds.SoundMapper;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.util.BooleanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jakepiccolo on 2/18/16.
 */
@RegisterMapper(SoundMapper.class)
public abstract class SoundDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundDAO.class);

    protected static final Integer OLD_FW_VERSION_CUTOFF = 100000000;

    @SqlQuery("SELECT * FROM sleep_sounds WHERE id = :id LIMIT 1;")
    @SingleValueResult(Sound.class)
    public abstract Optional<Sound> getById(@Bind("id") final Long id);

    @SqlQuery("SELECT * FROM sleep_sounds ORDER BY sort_key;")
    public abstract List<Sound> all();

    @SqlQuery("SELECT * FROM sleep_sounds WHERE firmware_version <= :firmware_version ORDER BY sort_key;")
    protected abstract List<Sound> getAllForFirmwareVersion(@Bind("firmware_version") final Integer firmwareVersion);

    public List<Sound> getAllForFirmwareVersionExcludingOldVersions(final Integer firmwareVersion) {
        if (firmwareVersion >= OLD_FW_VERSION_CUTOFF) {
            return Lists.newArrayList();
        }
        return getAllForFirmwareVersion(firmwareVersion);
    }

    @SqlQuery("SELECT * FROM sleep_sounds WHERE file_path = :file_path LIMIT 1;")
    @SingleValueResult(Sound.class)
    public abstract Optional<Sound> getByFilePath(@Bind("file_path") final String filePath);

    @SqlQuery("SELECT COUNT(*) > 0 FROM sleep_sounds WHERE id = :id AND firmware_version <= :firmware_version;")
    @RegisterMapper(BooleanMapper.class)
    protected abstract Boolean hasSoundEnabled(@Bind("id") final Long id,
                                               @Bind("firmware_version") final Integer firmwareVersion);

    public Boolean hasSoundEnabledExcludingOldFirmwareVersions(final Long id, final Integer firmwareVersion) {
        if (firmwareVersion >= OLD_FW_VERSION_CUTOFF) {
            return false;
        }
        return hasSoundEnabled(id, firmwareVersion);
    }
}
