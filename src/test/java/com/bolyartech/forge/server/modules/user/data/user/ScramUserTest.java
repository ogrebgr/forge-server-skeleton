package com.bolyartech.forge.server.modules.user.data.user;

import com.bolyartech.forge.server.config.DbConfiguration;
import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.misc.ServerTools;
import com.bolyartech.forge.server.modules.user.data.scram.Scram;
import com.bolyartech.forge.server.modules.user.data.scram.ScramDbh;
import com.bolyartech.forge.server.modules.user.data.scram.ScramDbhImpl;
import com.bolyartech.forge.server.modules.user.data.user_scram.UserScram;
import com.bolyartech.forge.server.modules.user.data.user_scram.UserScramDbh;
import com.bolyartech.forge.server.modules.user.data.user_scram.UserScramDbhImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;


public class ScramUserTest {
    private DbPool mDbPool;

    @Before
    public void setup() throws SQLException {
        if (mDbPool == null) {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("db.conf").getFile());

            DbConfiguration dbConf = ServerTools.loadDbConf(file);

            mDbPool = ServerTools.createComboPooledDataSource(dbConf);
        }

        Connection dbc = mDbPool.getConnection();
        DbTools.deleteAllUsers(dbc);
        DbTools.deleteAllScrams(dbc);
        dbc.close();
    }


    @Test
    public void testCreateNew() throws SQLException {
        ScramDbh scramDbh = new ScramDbhImpl();
        UserDbh userDbh = new UserDbhImpl();

        UserScramDbh dbh = new UserScramDbhImpl();

        Connection dbc = mDbPool.getConnection();
        UserScram scrNew = dbh.createNew(dbc, userDbh, scramDbh, "username", "salt", "server_key", "stored_key", 11);
        dbc.close();

        assertTrue("user not set", scrNew.getUser() != null);
        assertTrue("scram not set", scrNew.getScram() != null);
    }
}