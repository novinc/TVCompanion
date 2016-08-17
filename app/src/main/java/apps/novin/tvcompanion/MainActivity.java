package apps.novin.tvcompanion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
    @BindView(R.id.loading)
    ProgressBar progressBar;
    @BindView(R.id.ad_view)
    AdView adView;

    Snackbar make;

    boolean syncing = false;

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
    public static final long SYNC_INTERVAL_IN_MINUTES = 1440L; // once a day
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;
    private float elevation;


    private int currPage;
    private Fragment currentFragment;

    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean ads = true;

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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (ads) {
            AdRequest request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
                    //.addTestDevice("5B11814D0C43B2169F63811CB9BB055A")
                    .build();
            adView.loadAd(request);
        }

        Resources r = getResources();
        elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
        currPage = 0;

        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            mNavigationView.setCheckedItem(R.id.nav_find_shows);
            mNavigationView.getMenu().performIdentifierAction(R.id.nav_find_shows, 0);
        }
        mAccount = createSyncAccount(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // launch login if not logged in
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean loggedIn = preferences.getBoolean("loggedIn", false);
        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, 1);
        }
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
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void dataChanged(DatabaseUpdatedEvent e) {
        progressBar.setVisibility(View.INVISIBLE);
        if (make != null && make.isShown()) {
            make.dismiss();
        }
        syncing = false;

        ImageView profile = (ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.profile);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userPhoto = preferences.getString("user_photo", null);
        Glide.with(this).load(userPhoto).centerCrop().error(R.drawable.ic_close_black).into(profile);
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_name)).setText(preferences.getString("user_name", null));
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.last_synced)).setText(formatLastSync(preferences));

        if (e.showMessage()) {
            Snackbar.make(contentFragment, getString(R.string.sync_complete), Snackbar.LENGTH_LONG).show();
        }
        EventBus.getDefault().removeAllStickyEvents();
    }

    private String formatLastSync(SharedPreferences preferences) {
        long now = System.currentTimeMillis();
        long syncTime = preferences.getLong("sync_time", now);
        long milliDif = now - syncTime;
        String unit;
        int num;
        if (milliDif > 24 * 60 * 60 * 1000) {
            num = Math.round(((Long) milliDif).floatValue() / (24*60*60*1000));
            unit = getString(R.string.days);
        } else if (milliDif > 60 * 60 * 1000) {
            num = Math.round(((Long) milliDif).floatValue() / (60*60*1000));
            unit = getString(R.string.hours);
        } else if (milliDif > 60 * 1000) {
            num = Math.round(((Long) milliDif).floatValue() / (60*1000));
            unit = getString(R.string.minutes);
        } else if (milliDif > 1000) {
            num = Math.round(((Long) milliDif).floatValue() / (1000));
            unit = getString(R.string.seconds);
        } else if (milliDif != 0){
            num = Math.round(milliDif);
            unit = getString(R.string.milliseconds);
        } else {
            return getString(R.string.no_sync);
        }
        if (num == 1) {
            unit = unit.substring(0, unit.length() - 1);
        }
        return getString(R.string.last_sync_format, num, unit);
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        if (mNavigationView.getHeaderCount() == 0) {
            View header = mNavigationView.inflateHeaderView(R.layout.nav_header_main);
            ImageView imageView = (ImageView) header.findViewById(R.id.profile);
            if (imageView != null) {
                Glide.with(this).load(preferences.getString("user_photo", null)).centerCrop().error(R.drawable.ic_close_black).into(imageView);
            }
            ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_name)).setText(preferences.getString("user_name", null));
            ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.last_synced)).setText(formatLastSync(preferences));
        } else {
            View header = mNavigationView.getHeaderView(0);
            ImageView imageView = (ImageView) header.findViewById(R.id.profile);
            if (imageView != null) {
                Glide.with(this).load(preferences.getString("user_photo", null)).centerCrop().error(R.drawable.ic_close_black).into(imageView);
            }
            ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_name)).setText(preferences.getString("user_name", null));
            ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.last_synced)).setText(formatLastSync(preferences));
        }
        if (getIntent() != null) {
            boolean fromLogin = getIntent().getBooleanExtra("from_login", false);
            if (fromLogin) {
                if (!ContentResolver.isSyncPending(mAccount, AUTHORITY) && !ContentResolver.isSyncActive(mAccount, AUTHORITY)) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                    ContentResolver.requestSync(mAccount, AUTHORITY, bundle);
                }
                preferences.edit().putBoolean("syncing", true).apply();
                progressBar.setVisibility(View.VISIBLE);
                make = Snackbar.make(contentFragment, getString(R.string.full_sync_message), Snackbar.LENGTH_INDEFINITE);
                make.show();
                syncing = true;
                getIntent().removeExtra("from_login");
            }
            if (getIntent().getAction() != null && getIntent().getAction().contains(Intent.ACTION_VIEW)) {
                long id = getIntent().getLongExtra(ShowDetailActivity.ID_KEY, 0);
                DialogFragment dialogFragment = new ShowDetailDialog();
                Bundle bundle = new Bundle();
                bundle.putLong(ShowDetailDialog.ID_KEY, id);
                dialogFragment.setArguments(bundle);
                dialogFragment.show(getSupportFragmentManager(), "details");
            }
        }
        syncing = preferences.getBoolean("syncing", false);
        if (!syncing && make != null && make.isShown()) {
            progressBar.setVisibility(View.INVISIBLE);
            make.dismiss();
            Snackbar.make(contentFragment, getString(R.string.sync_complete), Snackbar.LENGTH_LONG).show();
            EventBus.getDefault().removeAllStickyEvents();
        } else if (syncing && make == null){
            progressBar.setVisibility(View.VISIBLE);
            make = Snackbar.make(contentFragment, getString(R.string.still_sync), Snackbar.LENGTH_INDEFINITE);
            make.show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View appBar = findViewById(R.id.appbar);
            if (currPage == 0) {
                appBar.postDelayed(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        appBar.setElevation(0);
                    }
                }, 400);
            } else {
                appBar.postDelayed(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        appBar.setElevation(elevation);
                    }
                }, 400);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("syncing", syncing);
        outState.putInt("page", currPage);
        getSupportFragmentManager().putFragment(outState, "fragment" + currPage, currentFragment);
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("CommitTransaction")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            mNavigationView.getMenu().findItem(R.id.nav_recommendation).setChecked(true);
        }
        if (savedInstanceState != null) {
            syncing = savedInstanceState.getBoolean("syncing", false);
            currPage = savedInstanceState.getInt("page", 0);
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, "fragment" + currPage);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, currentFragment).commitNow();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (currPage == 0) {
                    findViewById(R.id.appbar).setElevation(0);
                } else {
                    findViewById(R.id.appbar).setElevation(elevation);
                }
            }
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
            if (!syncing) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        SyncAdapter.syncShows(null, null, ((App) getApplication()).getDaoSession().getShowEntityDao(), getApplicationContext(), false);
                    }
                });
                progressBar.setVisibility(View.VISIBLE);
                make = Snackbar.make(contentFragment, getString(R.string.short_sync_message), Snackbar.LENGTH_INDEFINITE);
                make.show();
                syncing = true;
            }
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

        String title = null;

        if (id == R.id.nav_find_shows) {
            currentFragment = new FindShowsFragment();
            title = getString(R.string.nav_find_shows);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                findViewById(R.id.appbar).setElevation(0);
            }
            currPage = 0;
        } else if (id == R.id.nav_recommendation) {
            currentFragment = new RecommendationsFragment();
            title = getString(R.string.nav_recommendations);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                findViewById(R.id.appbar).setElevation(elevation);
            }
            currPage = 1;
        } else if (id == R.id.nav_my_shows) {
            currentFragment = new MyShowsFragment();
            title = getString(R.string.nav_my_shows);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                findViewById(R.id.appbar).setElevation(elevation);
            }
            currPage = 2;
        } else if (id == R.id.action_refresh) {
            if (!syncing) {
                ContentResolver.cancelSync(mAccount, AUTHORITY);
                ContentResolver.requestSync(mAccount, AUTHORITY, Bundle.EMPTY);
                progressBar.setVisibility(View.VISIBLE);
                make = Snackbar.make(contentFragment, getString(R.string.full_sync_message), Snackbar.LENGTH_INDEFINITE);
                make.show();
                syncing = true;
                if (!isDrawerLocked) {
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.closeDrawer(GravityCompat.START);
                }
                return true;
            }
            return false;
        } /*else if (id == R.id.nav_settings) { TODO settings and about
            return false;
        } else if (id == R.id.nav_about) {
            return false;
        }*/

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, currentFragment)
                .commitNow();

        invalidateOptionsMenu();

        // firebase analytics
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "MainDrawerTab");
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, title);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

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
            poster.setTransitionName(getString(R.string.poster_transition));
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView genre = (TextView) view.findViewById(R.id.genres);
            Pair<View, String> posterTrans = Pair.create((View) poster, getString(R.string.poster_transition));
            ActivityOptionsCompat options;
            options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(this, posterTrans);
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
