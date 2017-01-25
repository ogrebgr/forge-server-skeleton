package com.bolyartech.forge.server.modules.admin.data;

import com.bolyartech.forge.server.config.ForgeConfigurationException;
import com.bolyartech.forge.server.db.*;
import com.bolyartech.forge.server.modules.DbTools;
import com.bolyartech.scram_sasl.common.ScramUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;


public class AdminUserBlowfishDbhImplTest {
    private DbPool mDbPool;


    @Before
    public void setup() throws SQLException, ForgeConfigurationException {
        if (mDbPool == null) {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("conf/db.conf").getFile());

            DbConfigurationLoader loader = new FileDbConfigurationLoader();
            DbConfiguration dbConf = loader.load();

            mDbPool = DbUtils.createC3P0DbPool(dbConf);
        }

        Connection dbc = mDbPool.getConnection();
        DbTools.deleteAllAdminScrams(dbc);
        DbTools.deleteAllAdminUsers(dbc);

        dbc.close();
    }


    @Test
    public void testCreateNew() throws SQLException {
        Connection dbc = mDbPool.getConnection();

        AdminUserDbh adminUserDbh = new AdminUserDbhImpl();
        AdminScramDbhImpl scramDbh = new AdminScramDbhImpl();

        ScramUtils.NewPasswordStringData data = new ScramUtils.NewPasswordStringData("salted", "salt", "clientKey",
                "server_key", "stored_key", 11);

        AdminUserScramDbhImpl impl = new AdminUserScramDbhImpl();
        AdminUserScram obj = impl.createNew(dbc, adminUserDbh, scramDbh, true, "gele", "geleto", data);

        assertTrue("Not created", obj != null);
    }


    @Test
    public void testCreateNewFail() throws SQLException {
        Connection dbc = mDbPool.getConnection();

        AdminUserDbh adminUserDbh = new AdminUserDbhImpl();
        AdminScramDbhImpl scramDbh = new AdminScramDbhImpl();

        ScramUtils.NewPasswordStringData data = new ScramUtils.NewPasswordStringData("salted", "salt", "clientKey",
                "server_key", "stored_key", 11);


        AdminUserScramDbhImpl impl = new AdminUserScramDbhImpl();
        impl.createNew(dbc, adminUserDbh, scramDbh, true, "gele", "geleto", data);
        AdminUserScram obj2 = impl.createNew(dbc, adminUserDbh, scramDbh, true, "gele", "geleto", data);
        assertTrue("Created when it should not", obj2 == null);
    }
}