package apps.novin.tvcompanion;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
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

import java.util.List;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class MyShowsFragment extends Fragment {

    @BindView(R.id.my_shows_list_view)
    RecyclerView mRecyclerView;

    private RecyclerView.LayoutManager mLayoutManager;
    private MyShowsFragment.MyAdapter mAdapter;


    public MyShowsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_shows, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLayoutManager = new GridLayoutManager(getContext(), getContext().getResources().getInteger(R.integer.my_shows_span));
        mRecyclerView.setLayoutManager(mLayoutManager);
        List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).list();
        mAdapter = new MyAdapter(list);
        mRecyclerView.setAdapter(mAdapter);
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
        List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).list();
        mAdapter.setData(list);
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
            @BindView(R.id.poster)
            ImageView poster;

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
                    .inflate(R.layout.my_show_card, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            v.findViewById(R.id.card_view).setTag(vh);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ShowEntity showEntity = mDataset.get(position);
            Glide.with(MyShowsFragment.this)
                    .load(showEntity.getPoster_url())
                    .placeholder(R.drawable.show_background)
                    .error(R.drawable.ic_close_black)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.poster);
            holder.id = showEntity.getId();

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
