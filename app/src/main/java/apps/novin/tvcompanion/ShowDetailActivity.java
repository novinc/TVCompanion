package apps.novin.tvcompanion;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowDetailActivity extends AppCompatActivity {

    @BindView(R.id.scrollView)
    NestedScrollView scrollView;
    @BindView(R.id.poster)
    ImageView poster;
    @BindView(R.id.appbar)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.card_view_poster)
    CardView cardViewPoster;
    @BindView(R.id.seasons_spinner)
    Spinner spinner;
    @BindView(R.id.seasons_list)
    RecyclerView mRecyclerView;

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;

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
        }
        if (getIntent() != null) {
            id = getIntent().getLongExtra(ID_KEY, 0);
        }
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(new String[2]);
        mRecyclerView.setFocusable(false);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.getLayoutParams().height = mRecyclerView.getLayoutParams().height * 2;
        mRecyclerView.requestLayout();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.find_shows_tabs, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

    }



    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleStartPostponedTransition(poster);
        }
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

                } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
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
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }
    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private String[] mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            //@BindView(R.id.text)
            //TextView mTextView;

            public ViewHolder(View view) {
                super(view);
                //ButterKnife.bind(this, view);
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
                    .inflate(R.layout.recommendation_card, parent, false);
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
