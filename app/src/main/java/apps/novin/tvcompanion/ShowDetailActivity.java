package apps.novin.tvcompanion;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowDetailActivity extends AppCompatActivity {

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleStartPostponedTransition(poster);
        }

    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
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
                        return true;
                    }
                });
    }
}
