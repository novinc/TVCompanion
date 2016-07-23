package apps.novin.tvcompanion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "apps.novin.tvcompanion.db.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "tvcompanion.novin.apps";
    // The account name
    public static final String ACCOUNT = "TV companion";
    // Instance fields
    Account mAccount;

    // Sync interval constants
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 15L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;

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
        mAccount = createSyncAccount(this);
        /*
         * Turn on periodic syncing
         */
        ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // launch login if not logged in
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean loggedIn = preferences.getBoolean("loggedIn", false);
        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private static Account createSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        boolean success = accountManager.addAccountExplicitly(newAccount, null, null);
        if (success) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
        }
        return newAccount;
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

        if (id == R.id.action_refresh) {
            ContentResolver.requestSync(mAccount, AUTHORITY, Bundle.EMPTY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("CommitTransaction")
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;

        String title = null;

        if (id == R.id.nav_recommendation) {
            fragment = new RecommendationsFragment();
            title = getString(R.string.nav_recommendations);
        } else if (id == R.id.nav_find_shows) {
            fragment = new FindShowsFragment();
            title = getString(R.string.nav_find_shows);
        } else if (id == R.id.nav_my_shows) {
            fragment = new MyShowsFragment();
            title = getString(R.string.nav_my_shows);
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_about) {

        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commitNow();

        invalidateOptionsMenu();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (!isDrawerLocked) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public void onShowPressed(View view) {
        long id = 0;
        if (view.getTag() instanceof FindShowsPageFragment.MyAdapter.ViewHolder) {
            id = ((FindShowsPageFragment.MyAdapter.ViewHolder) view.getTag()).id;
        } else if (view.getTag() instanceof RecommendationsFragment.MyAdapter.ViewHolder) {
            id = ((RecommendationsFragment.MyAdapter.ViewHolder) view.getTag()).id;
        } else if (view.getTag() instanceof  MyShowsFragment.MyAdapter.ViewHolder) {
            id = ((MyShowsFragment.MyAdapter.ViewHolder) view.getTag()).id;
        }
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
            Bundle bundle = new Bundle();
            bundle.putLong(ShowDetailDialog.ID_KEY, id);
            dialogFragment.setArguments(bundle);
            dialogFragment.show(getSupportFragmentManager(), "details");
        } else {
            startActivity(intent);
        }
    }
}
