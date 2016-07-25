package apps.novin.tvcompanion;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.Stats;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.EpisodeEntity;
import apps.novin.tvcompanion.db.EpisodeEntityDao;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowDetailActivity extends AppCompatActivity {

    @BindView(R.id.appbar)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.backdrop)
    ImageView backdropImage;
    @BindView(R.id.card_view_poster)
    CardView cardViewPoster;
    @BindView(R.id.poster)
    ImageView poster;
    @BindView(R.id.add_fab)
    FloatingActionButton fab;
    @BindView(R.id.scrollView)
    NestedScrollView scrollView;
    @BindView(R.id.percentage)
    TextView percentage;
    @BindView(R.id.watchers)
    TextView watchers;
    @BindView(R.id.plays)
    TextView plays;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.year)
    TextView year;
    @BindView(R.id.genres)
    TextView genres;
    @BindView(R.id.description)
    TextView description;
    @BindView(R.id.seasons_spinner)
    Spinner spinner;
    @BindView(R.id.episodes_list)
    RecyclerView mRecyclerView;
    @BindView(R.id.heart_icon)
    ImageView heartIcon;
    @BindView(R.id.eye_icon)
    ImageView eyeIcon;

    private RecyclerView.LayoutManager mLayoutManager;
    private MyAdapter mAdapter;
    // used for poster shrinking
    private float px;

    private enum State {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    public static final String ID_KEY = "traktID";

    private long id;

    public ShowDetailActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_detail);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            scheduleStartPostponedTransition(poster);
        }
        if (getIntent() != null) {
            id = getIntent().getLongExtra(ID_KEY, 0);
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ShowEntityDao dao = ((App) getApplication()).getDaoSession().getShowEntityDao();
                final ShowEntity showEntity = dao.loadByRowId(id);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(ShowDetailActivity.this).load(showEntity.getPoster_url())
                                .placeholder(R.drawable.show_background)
                                .error(R.drawable.ic_close_black)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(poster);
                        Glide.with(ShowDetailActivity.this).load(showEntity.getBackdrop_url())
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(backdropImage);
                        title.setText(showEntity.getName());
                        genres.setText(showEntity.getGenres());
                        description.setText(showEntity.getDescription());
                        year.setText(String.format(Locale.ENGLISH, "%d", showEntity.getYear()));
                        if ((showEntity.getWatchers() != null && showEntity.getPlayers() != null) || showEntity.getPercent_heart() != 0) {
                            percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
                            watchers.setText(String.format(Locale.ENGLISH, "%s watchers", statFormat(showEntity.getWatchers())));
                            plays.setText(String.format(Locale.ENGLISH, "%s plays", statFormat(showEntity.getPlayers())));
                        } else {
                            heartIcon.setVisibility(View.INVISIBLE);
                            eyeIcon.setVisibility(View.INVISIBLE);
                            percentage.setVisibility(View.INVISIBLE);
                            watchers.setVisibility(View.INVISIBLE);
                            plays.setVisibility(View.INVISIBLE);
                            genres.setText(R.string.text_more_info);
                        }
                        fab.setImageResource(showEntity.getMy_show() ? R.drawable.ic_check_black : R.drawable.ic_add_black);
                    }
                });
                Resources r = getResources();
                px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, r.getDisplayMetrics());
                mLayoutManager = new LinearLayoutManager(ShowDetailActivity.this);
                mLayoutManager.setAutoMeasureEnabled(true);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setNestedScrollingEnabled(false);
                final EpisodeEntityDao episodeEntityDao = ((App) getApplication()).getDaoSession().getEpisodeEntityDao();
                mAdapter = new MyAdapter(new ArrayList<EpisodeEntity>(0));
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setFocusable(false);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        final Snackbar make = Snackbar.make(view, "One second", Snackbar.LENGTH_INDEFINITE);
                        make.show();
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                String accessToken = preferences.getString("access_token", null);
                                traktV2.accessToken(accessToken);
                                if (showEntity.getMy_show()) {
                                    SyncItems syncItems = new SyncItems();
                                    SyncShow syncShow = new SyncShow();
                                    ShowIds showIds = new ShowIds();
                                    showIds.trakt = (int) showEntity.getTrakt_id();
                                    syncShow.id(showIds);
                                    syncItems.shows(syncShow);
                                    boolean success = false;
                                    try {
                                        success = traktV2.sync().deleteItemsFromWatchedHistory(syncItems).execute().isSuccessful();
                                    } catch (IOException e) {
                                        try {
                                            success = traktV2.sync().deleteItemsFromWatchedHistory(syncItems).execute().isSuccessful();
                                        } catch (IOException e1) {
                                            try {
                                                success = traktV2.sync().deleteItemsFromWatchedHistory(syncItems).execute().isSuccessful();
                                            } catch (IOException e2) {
                                                e2.printStackTrace();
                                            }
                                        }
                                    }
                                    if (success) {
                                        showEntity.setMy_show(false);
                                        for (EpisodeEntity episodeEntity : showEntity.getEpisodeEntityList()) {
                                            episodeEntityDao.update(episodeEntity);
                                        }
                                        showEntity.update();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                make.dismiss();
                                                fab.setImageResource(R.drawable.ic_add_black);
                                                Snackbar.make(view, "Removed show and all episodes from watched history", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                        EventBus.getDefault().postSticky(new DatabaseUpdatedEvent());
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                make.dismiss();
                                                Snackbar.make(view, "Could not remove from trakt, try again later. Most likely due to connectivity issues", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                } else {
                                    SyncItems syncItems = new SyncItems();
                                    SyncShow syncShow = new SyncShow();
                                    ShowIds showIds = new ShowIds();
                                    showIds.trakt = (int) showEntity.getTrakt_id();
                                    syncShow.id(showIds);
                                    syncItems.shows(syncShow);
                                    boolean success = false;
                                    Show show = null;
                                    Stats stats = null;
                                    try {
                                        success = traktV2.sync().addItemsToWatchedHistory(syncItems).execute().isSuccessful();
                                        if (heartIcon.getVisibility() == View.INVISIBLE) {
                                            show = traktV2.shows().summary("" + showEntity.getTrakt_id(), Extended.FULLIMAGES).execute().body();
                                            stats = traktV2.shows().stats("" + showEntity.getTrakt_id()).execute().body();
                                        }
                                    } catch (IOException e) {
                                        try {
                                            if (!success) {
                                                success = traktV2.sync().addItemsToWatchedHistory(syncItems).execute().isSuccessful();
                                            }
                                            if (heartIcon.getVisibility() == View.INVISIBLE) {
                                                if (show == null) {
                                                    show = traktV2.shows().summary("" + showEntity.getTrakt_id(), Extended.FULLIMAGES).execute().body();
                                                }
                                                stats = traktV2.shows().stats("" + showEntity.getTrakt_id()).execute().body();
                                            }
                                        } catch (IOException e1) {
                                            try {
                                                if (!success) {
                                                    success = traktV2.sync().addItemsToWatchedHistory(syncItems).execute().isSuccessful();
                                                }
                                                if (heartIcon.getVisibility() == View.INVISIBLE) {
                                                    if (show == null) {
                                                        show = traktV2.shows().summary("" + showEntity.getTrakt_id(), Extended.FULLIMAGES).execute().body();
                                                    }
                                                    stats = traktV2.shows().stats("" + showEntity.getTrakt_id()).execute().body();
                                                }
                                            } catch (IOException e2) {
                                                e2.printStackTrace();
                                            }
                                        }
                                    }
                                    if (success) {
                                        if (show != null) {
                                            showEntity.setPercent_heart((int)(10 * show.rating));
                                            showEntity.setGenres("genres: " + show.genres.toString().replace("[", "").replace("]", ""));
                                            if (stats != null) {
                                                showEntity.setWatchers(stats.watchers.longValue());
                                                showEntity.setPlayers(stats.plays.longValue());
                                            }
                                            final Stats finalStats = stats;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    percentage.setVisibility(View.VISIBLE);
                                                    watchers.setVisibility(View.VISIBLE);
                                                    plays.setVisibility(View.VISIBLE);
                                                    genres.setText(showEntity.getGenres());
                                                    percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
                                                    heartIcon.setVisibility(View.VISIBLE);
                                                    if (finalStats != null) {
                                                        watchers.setText(String.format(Locale.ENGLISH, "%s watchers", statFormat(showEntity.getWatchers())));
                                                        plays.setText(String.format(Locale.ENGLISH, "%s plays", statFormat(showEntity.getPlayers())));
                                                        eyeIcon.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            });
                                            List<EpisodeEntity> episodesToInsert = new ArrayList<>();
                                            SyncAdapter.getEpisodesFor(showEntity, traktV2, episodeEntityDao, episodesToInsert);
                                            episodeEntityDao.insertInTx(episodesToInsert);
                                        }
                                        showEntity.setMy_show(true);
                                        for (EpisodeEntity episodeEntity : showEntity.getEpisodeEntityList()) {
                                            episodeEntityDao.update(episodeEntity);
                                        }
                                        showEntity.update();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                make.dismiss();
                                                fab.setImageResource(R.drawable.ic_close_black);
                                                Snackbar.make(view, "Added show and all episodes to watched history", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                        EventBus.getDefault().postSticky(new DatabaseUpdatedEvent());
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                make.dismiss();
                                                Snackbar.make(view, "Could not add to trakt, try again later. Most likely due to connectivity issues", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                });

                List<EpisodeEntity> all = episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(id)).orderAsc(EpisodeEntityDao.Properties.Season).list();
                if (all.size() > 0) {
                    final int seasonStart = all.get(0).getSeason();
                    int numSeasons = showEntity.getSeasons();
                    Log.d("details", "start " + seasonStart + " num " + numSeasons);
                    List<String> seasons = new ArrayList<>(numSeasons);
                    for (int i = seasonStart; i < numSeasons + seasonStart; i++) {
                        seasons.add("season " + i);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ShowDetailActivity.this, android.R.layout.simple_spinner_item, seasons);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            List<EpisodeEntity> list = episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(id), EpisodeEntityDao.Properties.Season.eq(i + seasonStart)).orderAsc(EpisodeEntityDao.Properties.Ep_number).list();
                            mAdapter.setData(list);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                } else {
                    spinner.setEnabled(false);
                }
            }
        });

    }

    private String statFormat(Long stat) {
        String suffix = "";
        try {
            if (stat > 1000 * 1000) {
                suffix = "m";
                stat /= 1000 * 1000;
            } else if (stat > 1000) {
                suffix = "k";
                stat /= 1000;
            }
            return stat + suffix;
        } catch (NullPointerException e) {
            return "0";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {

            private State state;

            @Override
            public synchronized void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset == 0) {
                    if (state != State.EXPANDED) {

                        ViewCompat.animate(cardViewPoster)
                                .setInterpolator(new OvershootInterpolator())
                                .scaleX(1)
                                .scaleY(1)
                                .start();

                    }
                    state = State.EXPANDED;

                } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange() - px) {
                    if (state != State.COLLAPSED) {
                        ViewCompat.animate(cardViewPoster)
                                .setInterpolator(new OvershootInterpolator())
                                .scaleX(0)
                                .scaleY(0)
                                .start();

                    }
                    state = State.COLLAPSED;
                } else {
                    if (state != State.IDLE) {
                        if (state == State.COLLAPSED) {
                            ViewCompat.animate(cardViewPoster)
                                    .setInterpolator(new OvershootInterpolator())
                                    .scaleX(1)
                                    .scaleY(1)
                                    .start();
                        }
                    }
                    state = State.IDLE;
                }
            }

        });
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }
    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<EpisodeEntity> mDataset;

        public void setData(List<EpisodeEntity> data) {
            this.mDataset = data;
            notifyDataSetChanged();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.episode_title)
            TextView title;
            @BindView(R.id.episode_desc)
            TextView description;
            @BindView(R.id.episode_poster)
            ImageView poster;
            @BindView(R.id.heart_button)
            Button heartButton;

            public ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<EpisodeEntity> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.episode_card, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            v.setTag(vh);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            EpisodeEntity episode = mDataset.get(position);
            holder.title.setText(String.format(Locale.ENGLISH, "%s %s", episode.getEp_name(), String.format(Locale.ENGLISH, "%dx%s", episode.getSeason(), episode.getEp_number())));
            holder.description.setText(episode.getEp_description());
            holder.heartButton.setText(String.format(Locale.ENGLISH, "%d%%", episode.getPercent_heart()));
            Glide.with(ShowDetailActivity.this)
                    .load(episode.getPoster_url())
                    .placeholder(R.color.colorAccent)
                    .error(R.drawable.ic_close_black)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.poster);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
