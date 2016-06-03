package com.bolyartech.forge.server.modules.admin.endpoints;

import com.bolyartech.forge.server.Handler;
import com.bolyartech.forge.server.HttpMethod;
import com.bolyartech.forge.server.StringEndpoint;
import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.misc.BasicResponseCodes;
import com.bolyartech.forge.server.misc.ForgeResponse;
import com.bolyartech.forge.server.misc.Params;
import com.bolyartech.forge.server.modules.admin.AdminHandler;
import com.bolyartech.forge.server.modules.admin.AdminResponseCodes;
import com.bolyartech.forge.server.modules.admin.data.AdminUser;
import com.bolyartech.forge.server.modules.user.data.User;
import spark.Request;
import spark.Response;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateAdminUserEp extends StringEndpoint {
    public CreateAdminUserEp(Handler<String> handler) {
        super(HttpMethod.POST, "create_admin_user", handler);
    }


    public static class CreateUserHandler extends AdminHandler {

        public CreateUserHandler(DbPool dbPool) {
            super(dbPool);
        }


        @Override
        protected ForgeResponse handleLoggedInAdmin(Request request, Response response, Connection dbc, AdminUser user) throws SQLException {
            if (user.isSuperAdmin()) {
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                String name = request.queryParams("name");
                String superAdminRaw = request.queryParams("super_admin");

                if (Params.areAllPresent(username, password, name)) {
                    if (!User.isValidUsername(username)) {
                        return new ForgeResponse(AdminResponseCodes.Errors.INVALID_USERNAME.getCode(), "Invalid username");
                    }

                    if (!AdminUser.isValidName(name)) {
                        return new ForgeResponse(AdminResponseCodes.Errors.INVALID_NAME.getCode(), "Invalid screen name");
                    }

                    if (!AdminUser.isValidPasswordLength(password)) {
                        return new ForgeResponse(AdminResponseCodes.Errors.PASSWORD_TOO_SHORT.getCode(), "Invalid screen name");
                    }

                    boolean superAdmin;
                    if (superAdminRaw != null) {
                        superAdmin = superAdminRaw.equals("1");
                    } else {
                        superAdmin = false;
                    }


                    try {
                        Statement lockSt = dbc.createStatement();
                        lockSt.execute("LOCK TABLES admin_users WRITE");

                        if (AdminUser.usernameExists(dbc, username)) {
                            return new ForgeResponse(AdminResponseCodes.Errors.USERNAME_EXISTS.getCode(), "username taken");
                        }

                        AdminUser.createNew(dbc, username, password, false, superAdmin, name);

                        return new ForgeResponse(BasicResponseCodes.Oks.OK.getCode(), "OK");
                    } finally {
                        Statement unlockSt = dbc.createStatement();
                        unlockSt.execute("UNLOCK TABLES");
                    }
                } else {
                    return new ForgeResponse(BasicResponseCodes.Errors.MISSING_PARAMETERS.getCode(), "Missing parameters");
                }
            } else {
                return new ForgeResponse(AdminResponseCodes.Errors.NO_ENOUGH_PRIVILEGES.getCode(), "Missing parameters");
            }
        }
    }
}