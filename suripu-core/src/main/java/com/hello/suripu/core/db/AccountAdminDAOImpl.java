package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.models.Account;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RegisterMapper(AccountMapper.class)
public abstract class AccountAdminDAOImpl implements AccountAdminDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAdminDAOImpl.class);

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE name ILIKE '%'||:name||'%' ORDER BY id DESC LIMIT 50;")
    public abstract List<Account> getAccountsByNameHint(
            @Bind("name") final String name
    );

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE email ILIKE '%'||:email||'%' ORDER BY id DESC LIMIT 50;")
    public abstract List<Account> getAccountsByEmailHint(
            @Bind("email") final String email
    );
}
