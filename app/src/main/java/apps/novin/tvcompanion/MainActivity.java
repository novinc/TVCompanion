package apps.novin.tvcompanion;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.content_frame)
    FrameLayout contentFragment;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.nav_view)
    NavigationView mNavigationView;

    private enum ScreenType {
        PHONE, SMALL_TABLET, SMALL_TABLET_LAND, BIG_TABLET
    }

    private ScreenType screenType;

    private boolean isDrawerLocked;

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        String screen = getString(R.string.screen_type);
        switch (screen) {
            case "Phone":
                screenType = ScreenType.PHONE;
                break;
            case "Small Tablet":
                screenType = ScreenType.SMALL_TABLET;
                break;
            case "Small Tablet Landscape":
                screenType = ScreenType.SMALL_TABLET_LAND;
                break;
            case "Big Tablet":
                screenType = ScreenType.BIG_TABLET;
                break;
        }
        setSupportActionBar(mToolbar);

        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            mNavigationView.setCheckedItem(R.id.nav_recommendation);
            mNavigationView.getMenu().performIdentifierAction(R.id.nav_recommendation, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screenType.equals(ScreenType.BIG_TABLET) || screenType.equals(ScreenType.SMALL_TABLET_LAND)) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            mDrawerLayout.setScrimColor(0x00000000);
            ((CustomDrawerLayout) mDrawerLayout).setDrawerViewWithoutIntercepting(mNavigationView);
            isDrawerLocked = true;
            if (mDrawerToggle != null) {
                mDrawerToggle.syncState();
            }
        } else {
            mDrawerToggle = new ActionBarDrawerToggle(
                    this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            mDrawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            ((CustomDrawerLayout) mDrawerLayout).setDrawerViewWithoutIntercepting(null);
            isDrawerLocked = false;
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            mNavigationView.getMenu().findItem(R.id.nav_recommendation).setChecked(true);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START) && !isDrawerLocked) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        getSupportFragmentManager().findFragmentById(R.id.content_frame).onCreateOptionsMenu(menu, getMenuInflater());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;

        if (id == R.id.nav_recommendation) {
            fragment = new RecommendationsFragment();
        } else if (id == R.id.nav_find_shows) {
            fragment = new FindShowsFragment();
        } else if (id == R.id.nav_my_shows) {
            fragment = new MyShowsFragment();
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_about) {

        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commitNow();

        invalidateOptionsMenu();

        if (!isDrawerLocked) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public void onShowPressed(View view) {
        long id = 0;
        Intent intent = new Intent(this, ShowDetailActivity.class);
        intent.putExtra(ShowDetailActivity.ID_KEY, id);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // disables big tablet dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && screenType != ScreenType.BIG_TABLET) {
            ImageView poster = (ImageView) view.findViewById(R.id.poster);
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView genre = (TextView) view.findViewById(R.id.genres);
            Pair<View, String> posterTrans = Pair.create((View) poster, getString(R.string.poster_transition));
            Pair<View, String> titleTrans = null;
            Pair<View, String> genreTrans = null;
            if (title != null) {
                //titleTrans = Pair.create((View) title, getString(R.string.title_transition));
            }
            if (genre != null) {
                //genreTrans = Pair.create((View) genre, getString(R.string.genre_transition));
            }
            ActivityOptionsCompat options;
            if (titleTrans == null && genreTrans == null) {
                options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(this, posterTrans);

            } else {
                options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(this, posterTrans, titleTrans, genreTrans);
            }
            startActivity(intent, options.toBundle());

        } else if (screenType == ScreenType.BIG_TABLET) {
            DialogFragment dialogFragment = new ShowDetailDialog();
            dialogFragment.show(getSupportFragmentManager(), "details");
        } else {
            startActivity(intent);
        }
    }
}
