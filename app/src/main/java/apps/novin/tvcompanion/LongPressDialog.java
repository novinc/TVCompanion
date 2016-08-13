package apps.novin.tvcompanion;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import apps.novin.tvcompanion.db.EpisodeEntity;
import apps.novin.tvcompanion.db.EpisodeEntityDao;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;

/**
 * Created by ncnov on 8/9/2016.
 */
public class LongPressDialog extends DialogFragment {

    Button browser;
    Button watchlist;
    Button recommendationRemove;

    int id;
    private int from;

    ShowEntityDao showEntityDao;
    EpisodeEntityDao episodeEntityDao;

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
        episodeEntityDao = ((App) getActivity().getApplication()).getDaoSession().getEpisodeEntityDao();

        View view = inflater.inflate(R.layout.dialog_long_press, null);
        browser = (Button) view.findViewById(R.id.long_press_browser);
        watchlist = (Button) view.findViewById(R.id.long_press_add_watchlist);
        recommendationRemove = (Button) view.findViewById(R.id.remove_recommendation);
        builder.setView(view);
        builder.setTitle(getArguments().getString("title"));
        id = getArguments().getInt("id");
        from = getArguments().getInt("from");
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
                String url = "https://trakt.tv/search/trakt/" + id + "?id_type=show";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                LongPressDialog.this.dismiss();
                startActivity(intent);
            }
        });
        watchlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LongPressDialog.this.dismiss();
                final View rootView = getActivity().findViewById(R.id.content_frame);
                final View progressBar = getActivity().findViewById(R.id.loading);
                progressBar.setVisibility(View.VISIBLE);
                final Snackbar snackbar = Snackbar.make(rootView, R.string.adding_watchlist, Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
                String accessToken = preferences.getString("access_token", null);
                if (accessToken != null) {
                    traktV2.accessToken(accessToken);
                    // make sync items for the show and add to watchlist
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            SyncItems items = new SyncItems();
                            SyncShow syncShow = new SyncShow();
                            ShowIds ids = new ShowIds();
                            ids.trakt = id;
                            syncShow.id(ids);
                            items.shows(syncShow);
                            int count = 0;
                            int maxTries = 3;
                            SyncResponse body = null;
                            while(true) {
                                try {
                                    body = traktV2.sync().addItemsToWatchlist(items).execute().body();
                                    break;
                                } catch (Exception e) {
                                    if (++count == maxTries) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            }
                            if (body != null) {
                                boolean successful = false;
                                count = 0;
                                maxTries = 3;
                                while(true) {
                                    try {
                                        successful = traktV2.recommendations().dismissShow("" + id).execute().isSuccessful();
                                        break;
                                    } catch (Exception e) {
                                        if (++count == maxTries) {
                                            e.printStackTrace();
                                            break;
                                        }
                                    }
                                }
                                if (!successful) {
                                    snackbar.dismiss();
                                    Snackbar.make(rootView, R.string.remove_recommendation_fail, Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                List<Show> recommendations;
                                count = 0;
                                maxTries = 3;
                                while(true) {
                                    try {
                                        recommendations = traktV2.recommendations().shows(Extended.FULLIMAGES).execute().body();
                                        break;
                                    } catch (Exception e) {
                                        if (++count == maxTries) e.printStackTrace();
                                    }
                                }
                                final List<ShowEntity> recommendationsToInsert = new ArrayList<>();
                                for (Show show : recommendations) {
                                    SyncAdapter.getStatsForRecommendationAndUpdatePositions(show, recommendationsToInsert, recommendations, traktV2, showEntityDao);
                                }
                                showEntityDao.insertInTx(recommendationsToInsert);
                                List<EpisodeEntity> episodesToInsert = new ArrayList<>();
                                for (ShowEntity showEntity : recommendationsToInsert) {
                                    SyncAdapter.getEpisodesFor(showEntity, traktV2, episodeEntityDao, episodesToInsert, false);
                                }
                                episodeEntityDao.insertInTx(episodesToInsert);
                                snackbar.dismiss();
                                Snackbar.make(rootView, R.string.watchlist_added, Snackbar.LENGTH_LONG).show();
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.INVISIBLE);
                                    }
                                });
                                EventBus.getDefault().postSticky(new DatabaseUpdatedEvent(false));
                            }
                        }
                    });
                }
            }
        });
        if (from == 1) {
            recommendationRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LongPressDialog.this.dismiss();
                    final View rootView = getActivity().findViewById(R.id.content_frame);
                    final View progressBar = getActivity().findViewById(R.id.loading);
                    progressBar.setVisibility(View.VISIBLE);
                    final Snackbar snackbar = Snackbar.make(rootView, R.string.updating_recommendations, Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    final TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
                    String accessToken = preferences.getString("access_token", null);
                    if (accessToken != null) {
                        traktV2.accessToken(accessToken);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                boolean successful = false;
                                int count = 0;
                                int maxTries = 3;
                                while(true) {
                                    try {
                                        successful = traktV2.recommendations().dismissShow("" + id).execute().isSuccessful();
                                        break;
                                    } catch (Exception e) {
                                        if (++count == maxTries) {
                                            e.printStackTrace();
                                            break;
                                        }
                                    }
                                }
                                if (!successful) {
                                    snackbar.dismiss();
                                    Snackbar.make(rootView, R.string.remove_recommendation_fail, Snackbar.LENGTH_LONG).show();
                                    return;
                                }
                                List<Show> recommendations;
                                count = 0;
                                maxTries = 3;
                                while(true) {
                                    try {
                                        recommendations = traktV2.recommendations().shows(Extended.FULLIMAGES).execute().body();
                                        break;
                                    } catch (Exception e) {
                                        if (++count == maxTries) e.printStackTrace();
                                    }
                                }
                                final List<ShowEntity> recommendationsToInsert = new ArrayList<>();
                                for (Show show : recommendations) {
                                    SyncAdapter.getStatsForRecommendationAndUpdatePositions(show, recommendationsToInsert, recommendations, traktV2, showEntityDao);
                                }
                                showEntityDao.insertInTx(recommendationsToInsert);
                                List<EpisodeEntity> episodesToInsert = new ArrayList<>();
                                for (ShowEntity showEntity : recommendationsToInsert) {
                                    SyncAdapter.getEpisodesFor(showEntity, traktV2, episodeEntityDao, episodesToInsert, false);
                                }
                                episodeEntityDao.insertInTx(episodesToInsert);
                                snackbar.dismiss();
                                Snackbar.make(rootView, R.string.recommendations_updated, Snackbar.LENGTH_LONG).show();
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.INVISIBLE);
                                    }
                                });
                                EventBus.getDefault().postSticky(new DatabaseUpdatedEvent(false));
                            }
                        });
                    }
                }
            });
        } else {
            recommendationRemove.setEnabled(false);
        }
    }

}
