package apps.novin.tvcompanion;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.google.android.gms.ads.MobileAds;

import apps.novin.tvcompanion.db.DaoMaster;
import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.EpisodeEntityContentProvider;
import apps.novin.tvcompanion.db.ShowEntityContentProvider;

/**
 * Main application class that gives access to the daoSession
 */
public class App extends Application {

    private DaoSession daoSession;

    private boolean isNightModeEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6177115838392476~7717129544");
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "shows-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        ShowEntityContentProvider.daoSession = daoSession;
        EpisodeEntityContentProvider.daoSession = daoSession;

        // We load the Night Mode state here
        SharedPreferences mPrefs =  PreferenceManager.getDefaultSharedPreferences(this);
        this.isNightModeEnabled = mPrefs.getBoolean("night_mode", false);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public boolean isNightModeEnabled() {
        return isNightModeEnabled;
    }
    public void setIsNightModeEnabled(boolean isNightModeEnabled) {
        this.isNightModeEnabled = isNightModeEnabled;
    }
}
