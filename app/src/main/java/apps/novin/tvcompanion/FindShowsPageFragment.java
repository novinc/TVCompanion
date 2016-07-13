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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    static final int MOST_PLAYED = 2;
    static final int SEARCH = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TRENDING, MOST_POPULAR, MOST_PLAYED, SEARCH})
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
        mAdapter = new MyAdapter(new String[100]);
        mRecyclerView.setAdapter(mAdapter);
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private String[] mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
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

            public ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(String[] myDataset) {
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
            v.setTag(vh);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            //holder.mTextView.setText(mDataset[position]);

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }
}
