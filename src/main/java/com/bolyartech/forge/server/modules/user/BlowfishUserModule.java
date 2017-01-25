package com.bolyartech.forge.server.modules.user;

import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.module.HttpModule;
import com.bolyartech.forge.server.modules.user.data.blowfish.BlowfishDbh;
import com.bolyartech.forge.server.modules.user.data.screen_name.ScreenNameDbh;
import com.bolyartech.forge.server.modules.user.data.user.UserDbh;
import com.bolyartech.forge.server.modules.user.data.user_blowfish.UserBlowfishDbh;
import com.bolyartech.forge.server.modules.user.endpoints.blowfish.AutoregistrationBfEp;
import com.bolyartech.forge.server.modules.user.endpoints.blowfish.LoginBfEp;
import com.bolyartech.forge.server.modules.user.endpoints.blowfish.RegistrationBfEp;
import com.bolyartech.forge.server.modules.user.endpoints.blowfish.RegistrationPostAutoBfEp;
import com.bolyartech.forge.server.route.PostRoute;
import com.bolyartech.forge.server.route.Route;

import java.util.ArrayList;
import java.util.List;


public class BlowfishUserModule implements HttpModule {
    private static final String DEFAULT_PATH_PREFIX = "/api/user/bf/";

    private static final String MODULE_SYSTEM_NAME = "user_blowfish";
    private static final int MODULE_VERSION_CODE = 1;
    private static final String MODULE_VERSION_NAME = "1.0.0";

    private final String mPathPrefix;
    private final DbPool mDbPool;
    private final UserBlowfishDbh mUserBlowfishDbh;
    private final UserDbh mUserDbh;
    private final BlowfishDbh mBlowfishDbh;
    private final ScreenNameDbh mScreenNameDbh;


    public BlowfishUserModule(String pathPrefix, DbPool dbPool, UserBlowfishDbh userBlowfishDbh, UserDbh userDbh,
                              BlowfishDbh blowfishDbh, ScreenNameDbh screenNameDbh) {
        mPathPrefix = pathPrefix;
        mDbPool = dbPool;
        mUserBlowfishDbh = userBlowfishDbh;
        mUserDbh = userDbh;
        mBlowfishDbh = blowfishDbh;
        mScreenNameDbh = screenNameDbh;
    }


    public BlowfishUserModule(DbPool dbPool, UserBlowfishDbh userBlowfishDbh, UserDbh userDbh, BlowfishDbh blowfishDbh,
                              ScreenNameDbh screenNameDbh) {


        this(DEFAULT_PATH_PREFIX, dbPool, userBlowfishDbh, userDbh, blowfishDbh, screenNameDbh);
    }


    @Override
    public List<Route> createRoutes() {
        List<Route> ret = new ArrayList<>();

        ret.add(new PostRoute(mPathPrefix + "autoregister",
                new AutoregistrationBfEp(mDbPool, mUserDbh, mBlowfishDbh, mUserBlowfishDbh)));
        ret.add(new PostRoute(mPathPrefix + "login",
                new LoginBfEp(mDbPool, mUserDbh, mBlowfishDbh, mScreenNameDbh)));
        ret.add(new PostRoute(mPathPrefix + "register",
                new RegistrationBfEp(mDbPool, mUserDbh, mBlowfishDbh, mUserBlowfishDbh, mScreenNameDbh)));
        ret.add(new PostRoute(mPathPrefix + "register_postauto",
                new RegistrationPostAutoBfEp(mDbPool, mUserDbh, mBlowfishDbh, mUserBlowfishDbh, mScreenNameDbh)));


        return ret;
    }


    @Override
    public String getSystemName() {
        return MODULE_SYSTEM_NAME;
    }


    @Override
    public String getShortDescription() {
        return "";
    }


    @Override
    public int getVersionCode() {
        return MODULE_VERSION_CODE;
    }


    @Override
    public String getVersionName() {
        return MODULE_VERSION_NAME;
    }
}
