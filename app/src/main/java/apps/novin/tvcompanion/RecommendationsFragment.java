package apps.novin.tvcompanion;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecommendationsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ShowEntity>> {

    @BindView(R.id.recommendations_list_view)
    RecyclerView mRecyclerView;

    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public RecommendationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recommendations, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(new ArrayList<ShowEntity>(0));
        mRecyclerView.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void dataChange(DatabaseUpdatedEvent event) {
        getLoaderManager().restartLoader(0, null, this).forceLoad();
        EventBus.getDefault().removeAllStickyEvents();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new Loader(getContext(), (App) getActivity().getApplication());
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<ShowEntity>> loader, List<ShowEntity> data) {
        if (mAdapter != null) {
            mAdapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<ShowEntity>> loader) {
        mAdapter = null;
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private List<ShowEntity> data;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.title)
            TextView title;
            @BindView(R.id.genres)
            TextView genres;
            @BindView(R.id.description)
            TextView description;
            @BindView(R.id.seasons)
            TextView seasons;
            @BindView(R.id.percentage)
            TextView percentage;
            @BindView(R.id.poster)
            ImageView poster;
            @BindView(R.id.card_view)
            CardView cardView;

            long id;

            public ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<ShowEntity> data) {
            this.data = data;
        }

        public void setData(List<ShowEntity> list) {
            this.data = list;
            notifyDataSetChanged();
            if (list.size() == 0) {
                final Snackbar make = Snackbar.make(RecommendationsFragment.this.getView(), R.string.no_recommendations, Snackbar.LENGTH_INDEFINITE);
                make.setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        make.dismiss();
                    }
                }).show();
            }
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recommendation_card, parent, false);
            ViewHolder vh = new ViewHolder(v);
            v.findViewById(R.id.card_view).setTag(vh);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final ShowEntity showEntity = data.get(position);
            Glide.with(RecommendationsFragment.this)
                    .load(showEntity.getPoster_url())
                    .placeholder(R.drawable.show_background)
                    .error(R.drawable.trakt)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.poster);
            holder.title.setText(showEntity.getName());
            holder.genres.setText(showEntity.getGenres());
            holder.description.setText(showEntity.getDescription());
            holder.seasons.setText(getString(R.string.seasons_format, showEntity.getSeasons()));
            holder.percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
            holder.id = showEntity.getId();
            holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    LongPressDialog dialog = new LongPressDialog();
                    Bundle bundle = new Bundle();
                    bundle.putString("title", holder.title.getText().toString());
                    bundle.putInt("id", (int) showEntity.getTrakt_id());
                    bundle.putInt("from", 1);
                    dialog.setArguments(bundle);
                    dialog.show(getActivity().getFragmentManager(), "dialog");
                    return true;
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    public static class Loader extends AsyncTaskLoader<List<ShowEntity>> {

        App application;

        public Loader(Context context, App application) {
            super(context);
            this.application = application;
        }

        @Override
        public List<ShowEntity> loadInBackground() {
            return application.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Recommendation.eq(true)).orderAsc(ShowEntityDao.Properties.Recommendation_pos).build().list();
        }
    }
}
