package apps.novin.tvcompanion;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

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
        View view = inflater.inflate(R.layout.activity_show_detail, container, false);
        ButterKnife.bind(this, view);
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
