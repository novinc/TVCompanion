package apps.novin.tvcompanion;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.TrendingShow;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;

/**
 * Created by ncnov on 7/17/2016.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final ContentResolver mContentResolver;
    private DaoSession mDaoSession;

    public SyncAdapter(Context context, boolean autoInitialize, DaoSession session) {
        super(context, autoInitialize);
        mDaoSession = session;
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d("sync adapter", "onPerformSync called authority: " + s);

        TraktV2 trakt = new TraktV2(BuildConfig.API_KEY);
        List<TrendingShow> trendingShows;
        try {
            trendingShows = trakt.shows().trending(1, 10, Extended.FULLIMAGES).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("sync", "couldn't get trending shows");
            return;
        }
        ShowEntityDao showEntityDao = mDaoSession.getShowEntityDao();
        for (TrendingShow show : trendingShows) {
            showEntityDao.insertOrReplace(getEntityFromShow(show.show));
        }
        EventBus.getDefault().post(new DatabaseUpdatedEvent());
    }
    private static ShowEntity getEntityFromShow(Show show) {
        return new ShowEntity(null, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                2, ((int) (show.rating * 100)),
                show.images.poster.thumb, show.images.banner.full, show.year, 5000, 10000,
                false, null, false, null, false, null, true);
    }
}
