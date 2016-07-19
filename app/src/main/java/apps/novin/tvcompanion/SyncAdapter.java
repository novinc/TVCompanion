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
        List<Show> popular;
        try {
            trendingShows = trakt.shows().trending(1, 10, Extended.FULLIMAGES).execute().body();
            popular = trakt.shows().popular(1, 10, Extended.FULLIMAGES).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("sync", "couldn't get trending shows");
            return;
        }
        ShowEntityDao showEntityDao = mDaoSession.getShowEntityDao();
        for (TrendingShow show : trendingShows) {
            List<ShowEntity> list = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.show.ids.trakt)).list();
            if (list.size() == 0) {
                showEntityDao.insert(getEntityFromTrendingShow(show.show, show, trendingShows.indexOf(show)));
            } else {
                ShowEntity showEntity = list.get(0);
                showEntity.setTrending(true);
                showEntity.setTrending_pos(trendingShows.indexOf(show));
                showEntity.update();
            }
        }
        for (Show show : popular) {
            List<ShowEntity> list = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.ids.trakt)).list();
            if (list.size() == 0) {
                showEntityDao.insertOrReplace(getEntityFromPopularShow(show, popular.indexOf(show)));
            } else {
                ShowEntity showEntity = list.get(0);
                showEntity.setMost_popular(true);
                showEntity.setMost_popular_pos(popular.indexOf(show));
                showEntity.update();
            }
        }
        EventBus.getDefault().post(new DatabaseUpdatedEvent());
    }

    private ShowEntity getEntityFromPopularShow(Show show, int i) {
        return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                2, ((int) (show.rating * 10)),
                show.images.poster.thumb, show.images.fanart.medium, show.year, 10000, 20000,
                false, null, true, i, true);
    }

    private static ShowEntity getEntityFromTrendingShow(Show show, TrendingShow trendingShow, int i) {
        return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                2, ((int) (show.rating * 10)),
                show.images.poster.thumb, show.images.fanart.medium, show.year, trendingShow.watchers, trendingShow.watchers,
                true, i, false, null, true);
    }
}
