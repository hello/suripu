package com.hello.suripu.service.resources;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.service.modules.RolloutModule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;

/**
 * Created by pangwu on 5/5/15.
 */
public class ResourceTest {
    @Mock protected GroupFlipper groupFlipper;
    @Mock protected KeyStore keyStore;
    @Mock protected MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    @Mock protected DataLogger dataLogger;
    @Mock protected KinesisLoggerFactory kinesisLoggerFactory;
    @Mock protected OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> oAuthTokenStore;
    @Mock protected DeviceDAO deviceDAO;
    @Mock protected ObjectGraphRoot objectGraphRoot;
    @Mock protected HttpServletRequest httpServletRequest;

    public void setUp(){
        MockitoAnnotations.initMocks(this);
        final RolloutModule module = new RolloutModule(mock(FeatureStore.class), 30);
        ObjectGraphRoot.getInstance().init(module);
    }
}
