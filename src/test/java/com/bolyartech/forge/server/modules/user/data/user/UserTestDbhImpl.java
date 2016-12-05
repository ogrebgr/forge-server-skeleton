package com.bolyartech.forge.server.modules.user.data.user;


import com.bolyartech.forge.server.config.DbConfiguration;
import com.bolyartech.forge.server.db.DbPool;
import com.bolyartech.forge.server.misc.ServerTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;


public class UserTestDbhImpl {
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
        DbTools.deleteAllScreenNames(dbc);
        DbTools.deleteAllUsers(dbc);
        dbc.close();
    }


    @After
    public void after() throws SQLException {
        Connection dbc = mDbPool.getConnection();
        DbTools.deleteAllUsers(dbc);
        dbc.close();
    }


    @Test
    public void testCreate() throws SQLException {
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);
        dbc.close();

        assertTrue("Not ID", userNew.getId() > 0);
        assertTrue("Not same data", userNew.isDisabled() &&
                UserLoginType.GOOGLE == userNew.getLoginType());
    }


    @Test
    public void testLoadById() throws SQLException {
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);
        User userLoaded = userDbh.loadById(dbc, userNew.getId());
        dbc.close();


        assertTrue("Not same data", userLoaded.equals(userNew));
    }


    @Test
    public void testChangeDisabled() throws SQLException {
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);
        User changed = userDbh.changeDisabled(dbc, userNew, false);
        dbc.close();

        assertTrue("not changed", !changed.isDisabled());
    }


    @Test
    public void testChangeLoginType() throws SQLException {
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);
        User changed = userDbh.changeLoginType(dbc, userNew, UserLoginType.FACEBOOK);
        dbc.close();

        assertTrue("not changed", changed.getLoginType() == UserLoginType.FACEBOOK);
    }


    @Test
    public void testExists() throws SQLException {
        UserDbh userDbh = new UserDbhImpl();

        Connection dbc = mDbPool.getConnection();
        assertTrue("User found when should not", !userDbh.exists(dbc, 1));
        User userNew = userDbh.createNew(dbc, true, UserLoginType.GOOGLE);
        assertTrue("User not found when should be", userDbh.exists(dbc, userNew.getId()));
        dbc.close();
    }
}