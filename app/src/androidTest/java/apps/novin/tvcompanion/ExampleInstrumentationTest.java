package apps.novin.tvcompanion;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import apps.novin.tvcompanion.db.DaoMaster;
import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.ShowEntity;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentationTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("apps.novin.tvcompanion", appContext.getPackageName());
    }
    @Test
    public void testDb() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(InstrumentationRegistry.getTargetContext(), "shows-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        daoSession.getShowEntityDao().deleteAll();
        daoSession.getShowEntityDao().insert(new ShowEntity(
                null, 5, "show name", "action", "long description", 5, 90, "poster url.com", "backdrop.com", 2011, null, null, false, null, false, null, false, null, false, false
        ));
        assertEquals(daoSession.getShowEntityDao().queryBuilder().list().size(), 1);
        assertEquals(daoSession.getShowEntityDao().queryBuilder().list().get(0).getName(), "show name");
        daoSession.getShowEntityDao().deleteAll();
    }
}