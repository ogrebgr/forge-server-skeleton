package com.bolyartech.forge.server.modules.user.data.user;

import com.bolyartech.forge.server.config.DbConfiguration;
import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.misc.ServerTools;
import com.bolyartech.forge.server.modules.user.data.scram.Scram;
import com.bolyartech.forge.server.modules.user.data.scram.ScramDbh;
import com.bolyartech.forge.server.modules.user.data.scram.ScramDbhImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;


public class ScramDbhImplTest {
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
        DbTools.deleteAllScrams(dbc);
        DbTools.deleteAllUsers(dbc);

        dbc.close();
    }


    @After
    public void after() throws SQLException {
        Connection dbc = mDbPool.getConnection();
        DbTools.deleteAllScrams(dbc);
        dbc.close();
    }


    @Test
    public void testCreateNew() throws SQLException {
        ScramDbh dbh = new ScramDbhImpl();
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);

        Scram scrNew = dbh.createNew(dbc, userNew.getId(), "username", "salt", "server_key", "stored_key", 11);
        Scram scrLoaded = dbh.loadByUser(dbc, userNew.getId());
        assertTrue("Created and loaded are different", scrLoaded.equals(scrNew));

        Scram scrNew2 = dbh.createNew(dbc, userNew.getId(), "username", "salt", "server_key", "stored_key", 11);
        assertTrue("duplicate username used", scrNew2 == null);

        dbc.close();
    }


    @Test
    public void testLoadByUsername() throws SQLException {
        ScramDbh dbh = new ScramDbhImpl();
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);

        Scram scrNew = dbh.createNew(dbc, userNew.getId(), "username", "salt", "server_key", "stored_key", 11);
        Scram scrLoaded = dbh.loadByUsername(dbc, "username");
        dbc.close();

        assertTrue("Created and loaded are different", scrLoaded.equals(scrNew));
    }


    @Test
    public void testReplace() throws SQLException {
        ScramDbh dbh = new ScramDbhImpl();
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);

        Scram scrNew = dbh.createNew(dbc, userNew.getId(), "username", "salt", "server_key", "stored_key", 11);
        Scram scrChanged = dbh.replace(dbc, scrNew, "newusername", "salt2", "server_key2", "stored_key2", 12);
        Scram scrLoaded = dbh.loadByUsername(dbc, "newusername");
        dbc.close();

        assertTrue("Changed and loaded are different", scrLoaded.equals(scrChanged));
    }
}