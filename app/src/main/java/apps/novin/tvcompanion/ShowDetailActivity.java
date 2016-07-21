package apps.novin.tvcompanion;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
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
                        percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
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
            @BindView(R.id.watched_button)
            Button watchedButton;
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
                    .placeholder(R.drawable.show_background)
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
