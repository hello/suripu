package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.mappers.AccountLocationMapper;
import com.hello.suripu.core.models.AccountLocation;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

/**
 * Created by kingshy on 12/30/15.
 */
@RegisterMapper(AccountLocationMapper.class)
public abstract class AccountLocationDAO {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_location (account_id, ip, latitude, longitude) VALUES " +
            "(:account_id, inet(:ip), :latitude, :longitude)")
    public abstract long insertNewAccountLatLongIP(@Bind("account_id") final Long accountId,
                                                   @Bind("ip") final String ip,
                                                   @Bind("latitude") final Double latitude,
                                                   @Bind("longitude") final Double longitude);

    @SqlUpdate("Update account_location SET city = :city, state = :state, country_code = :country_code " +
            "WHERE id = :location_id")
    public abstract int updateLocationNames(@Bind("location_id") final Long id,
                                            @Bind("city") final String city,
                                            @Bind("state") final String state,
                                            @Bind("country_code") final String countryCode);

    @SqlQuery("SELECT * FROM account_location WHERE account_id = :account_id ORDER BY created DESC LIMIT 1")
    @SingleValueResult(AccountLocation.class)
    public abstract Optional<AccountLocation> getLastLocationByAccountId(@Bind("account_id") final Long accountId);
}