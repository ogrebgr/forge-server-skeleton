package com.bolyartech.forge.server.modules.user.data.blowfish;

import java.sql.Connection;
import java.sql.SQLException;


public interface BlowfishDbh {
    Blowfish loadByUser(Connection dbc, long user) throws SQLException;

    Blowfish loadByUsername(Connection dbc, String username) throws SQLException;

    boolean usernameExists(Connection dbc, String username) throws SQLException;

    Blowfish createNew(Connection dbc, long user, String username, String passwordClearForm)
            throws SQLException;

    Blowfish replace(Connection dbc, long userId, String username, String passwordClearForm)
            throws SQLException;

    boolean changePassword(Connection dbc, long userId, String passwordClearForm)
            throws SQLException;
}
