package apps.novin.tvcompanion;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.jakewharton.processphoenix.ProcessPhoenix;
import com.uwetrottmann.trakt5.TraktV2;

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
                editor.putBoolean("loggedIn", false);
                editor.putString("access_token", null);
                editor.putString("refresh_token", null);
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
