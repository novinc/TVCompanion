package apps.novin.tvcompanion;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.enums.Type;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.EpisodeEntityDao;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FindShowsPageFragment#getInstance} factory method to
 * create an instance of this fragment.
 */
public class FindShowsPageFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ShowEntity>> {

    static final int TRENDING = 0;
    static final int MOST_POPULAR = 1;
    static final int SEARCH = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TRENDING, MOST_POPULAR, SEARCH})
    @interface TabMode {}
    private static final String TAB_MODE = "tab";
    private @TabMode int tabMode;

    @BindView(R.id.search_edit_text)
    @Nullable EditText searchText;

    @BindView(R.id.search_button)
    @Nullable Button searchButton;

    @BindView(R.id.find_shows_list_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.loading)
    @Nullable ProgressBar progressBar;

    private RecyclerView.LayoutManager mLayoutManager;
    private FindShowsPageFragment.MyAdapter mAdapter;

    private static FindShowsPageFragment[] fragments = new FindShowsPageFragment[3];


    public FindShowsPageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tabMode Parameter 1.
     * @return A new instance of fragment FindShowsPageFragment.
     */
    public static FindShowsPageFragment getInstance(@TabMode int tabMode) {
        if (fragments[tabMode] == null) {
            FindShowsPageFragment fragment = new FindShowsPageFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_MODE, tabMode);
            fragment.setArguments(args);
            fragments[tabMode] = fragment;
        }
        return fragments[tabMode];
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //noinspection WrongConstant
            tabMode = getArguments().getInt(TAB_MODE);
        }
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view;
        switch (tabMode) {
            case SEARCH:
                view = inflater.inflate(R.layout.fragment_find_shows_search_page, container, false);
                break;
            default:
                view = inflater.inflate(R.layout.fragment_find_shows_page, container, false);
                break;
        }
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (searchButton != null && searchText != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (searchText.getText().length() == 0) {
                        Toast.makeText(getContext(),"Enter title to search for", Toast.LENGTH_LONG).show();
                        return;
                    }
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    final String query = searchText.getText().toString();
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            final TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                            String accessToken = preferences.getString("access_token", null);
                            traktV2.accessToken(accessToken);
                            List<SearchResult> searchResults = null;
                            try {
                                searchResults = traktV2.search().textQuery(query, Type.SHOW, null, 1, 10).execute().body();
                            } catch (IOException e) {
                                try {
                                    searchResults = traktV2.search().textQuery(query, Type.SHOW, null, 1, 10).execute().body();
                                } catch (IOException e1) {
                                    try {
                                        searchResults = traktV2.search().textQuery(query, Type.SHOW, null, 1, 10).execute().body();
                                    } catch (IOException e2) {
                                        e2.printStackTrace();
                                    }
                                }
                            }
                            if (searchResults != null) {
                                final List<ShowEntity> showEntities = new ArrayList<>(searchResults.size());
                                final EpisodeEntityDao episodeEntityDao = ((App) getActivity().getApplication()).getDaoSession().getEpisodeEntityDao();
                                for (SearchResult result : searchResults) {
                                    ShowEntityDao showEntityDao = ((App) getActivity().getApplication()).getDaoSession().getShowEntityDao();
                                    List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(result.show.ids.trakt)).list();
                                    if (sameShows.size() == 0) {
                                        final ShowEntity showEntity;
                                        if (result.show.genres == null) {
                                            showEntity = new ShowEntity(null, result.show.ids.trakt, result.show.title, getString(R.string.genres), result.show.overview,
                                                    0, 0, result.show.images.poster.thumb, result.show.images.fanart.medium,
                                                    result.show.year, null, null,
                                                    false, null, false, null, false, null, false);
                                        } else {
                                            showEntity = new ShowEntity(null, result.show.ids.trakt, result.show.title, getString(R.string.genres) + result.show.genres.toString(), result.show.overview,
                                                    0, 0, result.show.images.poster.thumb, result.show.images.fanart.medium,
                                                    result.show.year, null, null,
                                                    false, null, false, null, false, null, false);
                                        }
                                        showEntities.add(showEntity);
                                        showEntityDao.insert(showEntity);
                                    } else {
                                        showEntities.add(sameShows.get(0));
                                    }

                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (progressBar != null) {
                                            progressBar.setVisibility(View.GONE);
                                        }
                                        mAdapter.setData(showEntities);
                                    }
                                });
                            }
                        }
                    });
                }
            });
            searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        searchButton.callOnClick();
                    }
                    return true;
                }
            });
        }
        mLayoutManager = new GridLayoutManager(getContext(), getContext().getResources().getInteger(R.integer.find_shows_span));
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(new ArrayList<ShowEntity>(0));
        mRecyclerView.setAdapter(mAdapter);
        getLoaderManager().initLoader(tabMode, null, this).forceLoad();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        if (mRecyclerView.getAdapter().getItemCount() == 0) {
            getLoaderManager().initLoader(tabMode, null, this).forceLoad();
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void dataChange(DatabaseUpdatedEvent event) {
        getLoaderManager().restartLoader(tabMode, null, this).forceLoad();
        EventBus.getDefault().removeAllStickyEvents();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new Loader(getContext(), tabMode, (App) getActivity().getApplication());
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<ShowEntity>> loader, List<ShowEntity> data) {
        if (mAdapter != null) {
            mAdapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<ShowEntity>> loader) {

    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<ShowEntity> mDataset;

        public void setData(List<ShowEntity> list) {
            mDataset = list;
            notifyDataSetChanged();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.title)
            TextView title;
            @BindView(R.id.genres)
            TextView genres;
            @BindView(R.id.year)
            TextView year;
            @BindView(R.id.percentage)
            TextView percentage;
            @BindView(R.id.poster)
            ImageView poster;
            @BindView(R.id.heart_icon)
            ImageView heartIcon;
            @BindView(R.id.card_view)
            CardView cardView;
            long id;

            public ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<ShowEntity> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.find_show_card, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            v.findViewById(R.id.card_view).setTag(vh);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final ShowEntity showEntity = mDataset.get(position);
            Glide.with(FindShowsPageFragment.this)
                    .load(showEntity.getPoster_url())
                    .placeholder(R.drawable.show_background)
                    .error(R.drawable.trakt_vert)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.poster);
            holder.title.setText(showEntity.getName());
            holder.genres.setText(showEntity.getGenres());
            holder.year.setText(showEntity.getYear() != null ? String.format(Locale.ENGLISH, "%d", showEntity.getYear()) : "");
            if (showEntity.getPercent_heart() != 0) {
                holder.percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
            } else {
                holder.percentage.setVisibility(View.INVISIBLE);
                holder.heartIcon.setVisibility(View.INVISIBLE);
                holder.genres.setVisibility(View.INVISIBLE);
            }
            holder.id = showEntity.getId();
            holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    LongPressDialog dialog = new LongPressDialog();
                    Bundle bundle = new Bundle();
                    bundle.putString("title", holder.title.getText().toString());
                    bundle.putInt("id", (int) showEntity.getTrakt_id());
                    bundle.putInt("from", 0);
                    dialog.setArguments(bundle);
                    dialog.show(getActivity().getFragmentManager(), "dialog");
                    return true;
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
    public static class Loader extends AsyncTaskLoader<List<ShowEntity>> {

        private int tabMode;
        private App app;

        public Loader(Context context, @TabMode int tabMode, App app) {
            super(context);
            this.tabMode = tabMode;
            this.app = app;
        }

        @Override
        public List<ShowEntity> loadInBackground() {
            List<ShowEntity> list = new ArrayList<>(0);
            if (tabMode == TRENDING) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Trending.eq(true)).orderAsc(ShowEntityDao.Properties.Trending_pos).build().list();
            } else if (tabMode == MOST_POPULAR) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Most_popular.eq(true)).orderAsc(ShowEntityDao.Properties.Most_popular_pos).build().list();
            } else { // search

            }
            return list;
        }
    }
}
