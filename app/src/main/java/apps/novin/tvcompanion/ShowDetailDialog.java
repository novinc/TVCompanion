package apps.novin.tvcompanion;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Created by ncnov on 7/11/2016.
 */

public class ShowDetailDialog extends DialogFragment {

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
    private ShowDetailDialog.MyAdapter mAdapter;
    // used for poster shrinking
    private float px;

    private enum State {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    public static final String ID_KEY = "traktID";

    private long id;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getLong(ID_KEY, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_show_detail, container, false);
        ButterKnife.bind(this, view);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ShowEntityDao dao = ((App) getActivity().getApplication()).getDaoSession().getShowEntityDao();
                final ShowEntity showEntity = dao.loadByRowId(id);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(ShowDetailDialog.this).load(showEntity.getPoster_url())
                                .placeholder(R.drawable.show_background)
                                .error(R.drawable.ic_close_black)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(poster);
                        Glide.with(ShowDetailDialog.this).load(showEntity.getBackdrop_url())
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
                mLayoutManager = new LinearLayoutManager(getContext());
                mLayoutManager.setAutoMeasureEnabled(true);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setNestedScrollingEnabled(false);
                final EpisodeEntityDao episodeEntityDao = ((App) getActivity().getApplication()).getDaoSession().getEpisodeEntityDao();
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, seasons);
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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {

            private ShowDetailDialog.State state;

            @Override
            public synchronized void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset == 0) {
                    if (state != ShowDetailDialog.State.EXPANDED) {

                        ViewCompat.animate(cardViewPoster)
                                .setInterpolator(new OvershootInterpolator())
                                .scaleX(1)
                                .scaleY(1)
                                .start();

                    }
                    state = ShowDetailDialog.State.EXPANDED;

                } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange() - px) {
                    if (state != ShowDetailDialog.State.COLLAPSED) {
                        ViewCompat.animate(cardViewPoster)
                                .setInterpolator(new OvershootInterpolator())
                                .scaleX(0)
                                .scaleY(0)
                                .start();

                    }
                    state = ShowDetailDialog.State.COLLAPSED;
                } else {
                    if (state != ShowDetailDialog.State.IDLE) {
                        if (state == ShowDetailDialog.State.COLLAPSED) {
                            ViewCompat.animate(cardViewPoster)
                                    .setInterpolator(new OvershootInterpolator())
                                    .scaleX(1)
                                    .scaleY(1)
                                    .start();
                        }
                    }
                    state = ShowDetailDialog.State.IDLE;
                }
            }

        });
    }
    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<EpisodeEntity> mDataset;

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

        public void setData(List<EpisodeEntity> mDataset) {
            this.mDataset = mDataset;
            notifyDataSetChanged();
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
            Glide.with(ShowDetailDialog.this)
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
