package apps.novin.tvcompanion;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FindShowsPageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FindShowsPageFragment extends Fragment {

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

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;


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
    public static FindShowsPageFragment newInstance(@TabMode int tabMode) {
        FindShowsPageFragment fragment = new FindShowsPageFragment();
        Bundle args = new Bundle();
        args.putInt(TAB_MODE, tabMode);
        fragment.setArguments(args);
        return fragment;
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
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
        mLayoutManager = new GridLayoutManager(getContext(), getContext().getResources().getInteger(R.integer.find_shows_span));
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (tabMode == TRENDING) {
            List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Trending.eq(true)).orderAsc(ShowEntityDao.Properties.Trending_pos).build().list();
            mAdapter = new MyAdapter(list);
        } else if (tabMode == MOST_POPULAR) {
            List<ShowEntity> list = ((App) getActivity().getApplication()).getDaoSession().queryBuilder(ShowEntity.class).where(ShowEntityDao.Properties.Most_popular.eq(true)).orderAsc(ShowEntityDao.Properties.Most_popular_pos).build().list();
            mAdapter = new MyAdapter(list);
        } else { // search

        }
        mRecyclerView.setAdapter(mAdapter);
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<ShowEntity> mDataset;

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
        public void onBindViewHolder(ViewHolder holder, int position) {
            ShowEntity showEntity = mDataset.get(position);
            Glide.with(FindShowsPageFragment.this)
                    .load(showEntity.getPoster_url())
                    .placeholder(R.drawable.show_background)
                    .error(R.drawable.ic_close_black)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.poster);
            holder.title.setText(showEntity.getName());
            holder.genres.setText(showEntity.getGenres());
            holder.year.setText(String.format(Locale.ENGLISH, "%d", showEntity.getYear()));
            holder.percentage.setText(String.format(Locale.ENGLISH, "%d", showEntity.getPercent_heart()));
            holder.id = showEntity.getId();
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
