package apps.novin.tvcompanion;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class MyShowsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ShowEntity>> {

    static final int AZ = 0;
    static final int RATING = 1;
    static final int NEW_OLD = 2;
    static final int OLD_NEW = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AZ, RATING, NEW_OLD, OLD_NEW})
    @interface SortMode {}
    private static final String SORT_MODE = "tab";
    private @SortMode int sortMode;

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_shows_menu, menu);
        MenuItem item = menu.findItem(R.id.sort_by);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(((AppCompatActivity) getActivity()).getSupportActionBar().getThemedContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter); // set the adapter to provide layout of rows and content
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) { // A-Z
                    sortMode = AZ;
                } else if (i == 1) { // rating
                    sortMode = RATING;
                } else if (i == 2) { // new - old
                    sortMode = NEW_OLD;
                } else if (i == 3) { // old - new
                    sortMode = OLD_NEW;
                }
                dataChange(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLayoutManager = new GridLayoutManager(getContext(), getContext().getResources().getInteger(R.integer.my_shows_span));
        mRecyclerView.setLayoutManager(mLayoutManager);
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
        mAdapter = new MyAdapter(new ArrayList<ShowEntity>(0));
        mRecyclerView.setAdapter(mAdapter);
        return new Loader(getContext(), sortMode, (App) getActivity().getApplication());
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

    public static class Loader extends AsyncTaskLoader<List<ShowEntity>> {

        private @SortMode int sortMode;
        private App app;

        public Loader(Context context, @SortMode int sortMode, App app) {
            super(context);
            this.sortMode = sortMode;
            this.app = app;
        }

        @Override
        public List<ShowEntity> loadInBackground() {
            List<ShowEntity> list = null;
            if (sortMode == AZ) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).orderAsc(ShowEntityDao.Properties.Name).list();
            } else if (sortMode == RATING) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).orderDesc(ShowEntityDao.Properties.Percent_heart).list();
            } else if (sortMode == NEW_OLD) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).orderDesc(ShowEntityDao.Properties.Year).list();
            } else if (sortMode == OLD_NEW) {
                list = app.getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.My_show.eq(true)).orderAsc(ShowEntityDao.Properties.Year).list();
            }
            return list;
        }
    }
}
