package apps.novin.tvcompanion;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import apps.novin.tvcompanion.db.DaoSession;

/**
 * Dialog for clearing cache / show episode data
 */
public class ClearCacheDialogPreference extends DialogPreference {

    public ClearCacheDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // Ok button press
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        DaoSession daoSession = ((App) getContext().getApplicationContext()).getDaoSession();
                        daoSession.getEpisodeEntityDao().deleteAll();
                        daoSession.getShowEntityDao().deleteAll();
                    }
                });
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                // Cancel button press
                break;
        }
        super.onClick(dialog, which);
    }
}
