package apps.novin.tvcompanion;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Since DialogPreference is abstract
 */
public class MyDialogPreference extends DialogPreference {

    public MyDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
