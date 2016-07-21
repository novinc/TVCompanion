package apps.novin.tvcompanion;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecommendationsFragment extends Fragment {

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
        List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Recommendation.eq(true)).orderAsc(ShowEntityDao.Properties.Recommendation_pos).build().list();
        mAdapter = new MyAdapter(list);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        dataChange(null);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void dataChange(DatabaseUpdatedEvent event) {
        List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Recommendation.eq(true)).orderAsc(ShowEntityDao.Properties.Recommendation_pos).build().list();
        mAdapter.setData(list);
        mAdapter.notifyDataSetChanged();
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

        public void setData(List<ShowEntity> data) {
            this.data = data;
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
        public void onBindViewHolder(ViewHolder holder, int position) {
            ShowEntity showEntity = data.get(position);
            Glide.with(RecommendationsFragment.this)
                    .load(showEntity.getPoster_url())
                    .placeholder(R.drawable.show_background)
                    .error(R.drawable.ic_close_black)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.poster);
            holder.title.setText(showEntity.getName());
            holder.genres.setText(showEntity.getGenres());
            holder.description.setText(showEntity.getDescription());
            holder.seasons.setText(getString(R.string.seasons_format, showEntity.getSeasons()));
            holder.percentage.setText(String.format(Locale.ENGLISH, "%d%%", showEntity.getPercent_heart()));
            holder.id = showEntity.getId();
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
