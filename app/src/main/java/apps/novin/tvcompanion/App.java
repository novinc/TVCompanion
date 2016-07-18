package apps.novin.tvcompanion;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import apps.novin.tvcompanion.db.DaoMaster;
import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.EpisodeEntityContentProvider;
import apps.novin.tvcompanion.db.ShowEntityContentProvider;

/**
 * Main application class that gives access to the daoSession
 */
public class App extends Application {

    DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "shows-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        ShowEntityContentProvider.daoSession = daoSession;
        EpisodeEntityContentProvider.daoSession = daoSession;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }
}
