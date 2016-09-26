package apps.novin.tvcompanion;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.jakewharton.processphoenix.ProcessPhoenix;

/**
 * Created by ncnov on 9/18/2016.
 */
public class LogoutDialogPreference extends DialogPreference{

    public LogoutDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // Ok button press
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(SyncPreferences.KEY_LOGGED_IN, false);
                editor.putString(SyncPreferences.KEY_ACCESS_TOKEN, null);
                editor.putString(SyncPreferences.KEY_REFRESH_TOKEN, null);
                editor.apply();
                ProcessPhoenix.triggerRebirth(getContext());
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                // Cancel button press
                break;
        }
        super.onClick(dialog, which);
    }
}
