package com.bolyartech.forge.server.modules.user;

import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.module.AbstractForgeModule;
import com.bolyartech.forge.server.register.StringEndpointRegister;
import com.bolyartech.forge.server.modules.user.endpoints.UserAutoregistrationEp;
import com.bolyartech.forge.server.modules.user.endpoints.LoginEp;
import com.bolyartech.forge.server.modules.user.endpoints.UserRegistrationEp;
import com.bolyartech.forge.server.modules.user.endpoints.UserRegistrationPostAutoEp;

public class UserModule extends AbstractForgeModule {
    private static final String MODULE_SYSTEM_NAME = "user";
    private static final int MODULE_VERSION_CODE = 1;
    private static final String MODULE_VERSION_NAME = "1.0.0";

    private final DbPool mDbPool;
    private final StringEndpointRegister mRegister;

    public UserModule(DbPool dbPool,
                      String sitePathPrefix,
                      StringEndpointRegister stringEndpointRegister) {

        this(dbPool, sitePathPrefix, stringEndpointRegister, "api/user/");
    }


    public UserModule(DbPool dbPool,
                      String sitePathPrefix,
                      StringEndpointRegister stringEndpointRegister,
                      String modulePathPrefix
                      ) {
        super(sitePathPrefix, modulePathPrefix);

        mDbPool = dbPool;
        mRegister = stringEndpointRegister;
    }


    @Override
    public void registerEndpoints() {
        String pathPrefix = getSitePathPrefix() + getModulePathPrefix();

        mRegister.register(pathPrefix,
                new UserAutoregistrationEp(new UserAutoregistrationEp.UserAutoregistrationHandler(mDbPool)));

        mRegister.register(pathPrefix,
                new UserRegistrationEp(new UserRegistrationEp.RegistrationHandler(mDbPool)));

        mRegister.register(pathPrefix, new LoginEp(new LoginEp.LoginHandler(mDbPool)));

        mRegister.register(pathPrefix,
                new UserRegistrationPostAutoEp(new UserRegistrationPostAutoEp.UserRegistrationPostAutoHandler(mDbPool)));
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
