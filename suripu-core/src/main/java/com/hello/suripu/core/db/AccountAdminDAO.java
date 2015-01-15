package com.hello.suripu.core.db;

import com.hello.suripu.core.models.Account;

import java.util.List;

public interface AccountAdminDAO {

    List<Account> getAccountsByNameHint( final String name );

    List<Account> getAccountsByEmailHint( final String email );

}
