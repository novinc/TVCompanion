package apps.novin.tvcompanion;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ncnov on 7/11/2016.
 */

public class ShowDetailDialog extends DialogFragment {


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
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ShowDetailActivity.MyAdapter(new String[5]);
        mRecyclerView.setFocusable(false);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.requestLayout();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.find_shows_tabs, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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

                } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
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
}
