package apps.novin.tvcompanion;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.test.mock.MockApplication;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.uwetrottmann.trakt5.TraktV2;

import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ncnov on 8/9/2016.
 */
public class LongPressDialog extends DialogFragment {

    Button browser;
    Button watchlist;
    Button recommendationRemove;

    long id;

    ShowEntityDao showEntityDao;

    public LongPressDialog() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Set a theme on the dialog builder constructor!
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity(), R.style.long_press_dialog);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        showEntityDao = ((App) getActivity().getApplication()).getDaoSession().getShowEntityDao();

        View view = inflater.inflate(R.layout.dialog_long_press, null);
        browser = (Button) view.findViewById(R.id.long_press_browser);
        watchlist = (Button) view.findViewById(R.id.long_press_add_watchlist);
        recommendationRemove = (Button) view.findViewById(R.id.remove_recommendation);
        builder.setView(view);
        builder.setTitle(getArguments().getString("title"));
        id = getArguments().getLong("id");
        setUpButtons();
        AlertDialog dialog = builder.create();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.5f);
        return dialog;
    }

    private void setUpButtons() {
        browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        watchlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
                String accessToken = preferences.getString("access_token", null);
                if (accessToken != null) {
                    traktV2.accessToken(accessToken);
                    // make sync items for the show and add to watchlist
                }
            }
        });
        recommendationRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

}
