package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielPublicAPI;
import org.iatoki.judgels.jophiel.controllers.JophielClientControllerUtils;
import org.iatoki.judgels.jophiel.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.sandalphon.jid.JidCacheDao;
import org.iatoki.judgels.sandalphon.jid.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.activity.ActivityLogDao;
import org.iatoki.judgels.sandalphon.activity.ActivityLogServiceImpl;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @deprecated Temporary class. Will be restructured when new module system has been finalized.
 */
@Singleton
@Deprecated
public final class SandalphonSingletonsBuilder {

    @Inject
    public SandalphonSingletonsBuilder(JidCacheDao jidCacheDao, ActivityLogDao activityLogDao, JophielClientAPI jophielClientAPI, JophielPublicAPI jophielPublicAPI) {
        JidCacheServiceImpl.buildInstance(jidCacheDao);
        ActivityLogServiceImpl.buildInstance(activityLogDao);
        UserActivityMessageServiceImpl.buildInstance();

        JophielClientControllerUtils.buildInstance(SandalphonProperties.getInstance().getJophielBaseUrl());
        SandalphonControllerUtils.buildInstance(jophielClientAPI, jophielPublicAPI);
    }
}
