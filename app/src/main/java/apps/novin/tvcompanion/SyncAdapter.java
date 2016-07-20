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
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.TrendingShow;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.EpisodeEntity;
import apps.novin.tvcompanion.db.EpisodeEntityDao;
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
            Log.e("sync", "couldn't get shows");
            return;
        }
        ShowEntityDao showEntityDao = mDaoSession.getShowEntityDao();
        EpisodeEntityDao episodeEntityDao = mDaoSession.getEpisodeEntityDao();
        // trending sync
        for (TrendingShow show : trendingShows) {
            List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.show.ids.trakt)).list();
            if (sameShows.size() == 0) {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trending_pos.eq(trendingShows.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setTrending(false);
                    samePos.get(0).setTrending_pos(null);
                    samePos.get(0).update();
                }
                showEntityDao.insert(getEntityFromTrendingShow(show.show, show, trendingShows.indexOf(show)));
            } else {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trending_pos.eq(trendingShows.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setTrending(false);
                    samePos.get(0).setTrending_pos(null);
                    samePos.get(0).update();
                }
                ShowEntity showEntity = sameShows.get(0);
                showEntity.setTrending(true);
                showEntity.setTrending_pos(trendingShows.indexOf(show));
                showEntity.update();
            }
        }
        // most popular sync
        for (Show show : popular) {
            List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.ids.trakt)).list();
            if (sameShows.size() == 0) {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Most_popular_pos.eq(popular.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setMost_popular(false);
                    samePos.get(0).update();
                }
                showEntityDao.insert(getEntityFromPopularShow(show, popular.indexOf(show)));
            } else {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Most_popular_pos.eq(popular.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setMost_popular(false);
                    samePos.get(0).update();
                }
                ShowEntity showEntity = sameShows.get(0);
                showEntity.setMost_popular(true);
                showEntity.setMost_popular_pos(popular.indexOf(show));
                showEntity.update();
            }
        }
        // episodes sync
        for (ShowEntity showEntity : showEntityDao.loadAll()) {
            // load up all the seasons for show and all episodes for each season
            try {
                List<Season> seasons = trakt.seasons().summary(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), Extended.FULL).execute().body();
                showEntity.setSeasons(seasons.size());
                showEntity.update();
                for (Season season : seasons) {
                    if (season.aired_episodes == episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(showEntity.getId()), EpisodeEntityDao.Properties.Season.eq(season.number)).count()) {
                        continue;
                    }
                    List<Episode> episodes = trakt.seasons().season(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), season.number, Extended.FULLIMAGES).execute().body();
                    for (Episode episode : episodes) {
                        long alreadyhave = episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(showEntity.getId()), EpisodeEntityDao.Properties.Season.eq(season.number),
                                EpisodeEntityDao.Properties.Ep_number.eq(episode.number)).count();
                        if (alreadyhave != 1) {
                            episodeEntityDao.insert(getEpisodeEntity(episode, showEntity.getId()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("sync", "couldn't get seasons/episodes");
                return;
            }
        }
        Log.d("sync", "sync complete");
        EventBus.getDefault().post(new DatabaseUpdatedEvent());
    }

    private static EpisodeEntity getEpisodeEntity(Episode episode, Long id) {
        return new EpisodeEntity(null, id, episode.season, episode.title, episode.number, episode.overview, false, ((int) (episode.rating * 10)), true, episode.images.screenshot.medium);
    }

    private static ShowEntity getEntityFromPopularShow(Show show, int i) {
        return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                2, ((int) (show.rating * 10)),
                show.images.poster.thumb, show.images.fanart.medium, show.year, 10000, 20000,
                false, null, true, i, true, false);
    }

    private static ShowEntity getEntityFromTrendingShow(Show show, TrendingShow trendingShow, int i) {
        return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                2, ((int) (show.rating * 10)),
                show.images.poster.thumb, show.images.fanart.medium, show.year, trendingShow.watchers, trendingShow.watchers,
                true, i, false, null, true, false);
    }
}
