package apps.novin.tvcompanion;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.Stats;
import com.uwetrottmann.trakt5.entities.TrendingShow;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
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

        TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String accessToken = preferences.getString("access_token", null);
        boolean oauth = false;
        if (accessToken != null) {
            traktV2.accessToken(accessToken);
            oauth = true;
        }
        Log.d("Sync", "oauth: " + oauth);
        List<BaseShow> myShows = null;
        List<Show> recommendations = null;
        List<TrendingShow> trendingShows;
        List<Show> popular;
        int count;
        int maxTries;
        count = 0;
        maxTries = 3;
        while(true) {
            try {
                if (oauth) {
                    myShows = traktV2.sync().watchedShows(Extended.FULLIMAGES).execute().body();
                    recommendations = traktV2.recommendations().shows(Extended.FULLIMAGES).execute().body();
                }
                trendingShows = traktV2.shows().trending(1, 10, Extended.FULLIMAGES).execute().body();
                popular = traktV2.shows().popular(1, 10, Extended.FULLIMAGES).execute().body();
                break;
            } catch (IOException e) {
                // handle exception
                if (++count == maxTries) try {
                    throw e;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Log.e("sync", "couldn't get shows");
                }
            }
        }
        ShowEntityDao showEntityDao = mDaoSession.getShowEntityDao();
        EpisodeEntityDao episodeEntityDao = mDaoSession.getEpisodeEntityDao();
        // recommendation sync
        if (oauth) {
            List<ShowEntity> myShowsToInsert = new ArrayList<>();
            for (BaseShow show : myShows) {
                Stats stats = null;
                count = 0;
                maxTries = 3;
                while(true) {
                    try {
                        stats = traktV2.shows().stats(show.show.ids.trakt.toString()).execute().body();
                        break;
                    } catch (IOException e) {
                        // handle exception
                        if (++count == maxTries) try {
                            throw e;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            Log.e("Sync", "couldn't get show stats");
                        }
                    }
                }
                List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.show.ids.trakt)).list();
                if (sameShows.size() == 0) { // newly watched show not in app's database
                    ShowEntity newShow = getEntityFromMyShow(show, stats);
                    myShowsToInsert.add(newShow);
                } else {
                    sameShows.get(0).setMy_show(true);
                    if (stats != null) {
                        sameShows.get(0).setWatchers(stats.watchers.longValue());
                        sameShows.get(0).setPlayers(stats.plays.longValue());
                    }
                    sameShows.get(0).setSynced(true);
                    sameShows.get(0).update();
                }
            }
            showEntityDao.insertInTx(myShowsToInsert);
            List<ShowEntity> recommendationsToInsert = new ArrayList<>();
            for (Show show : recommendations) {
                Stats stats = null;
                count = 0;
                maxTries = 3;
                while(true) {
                    try {
                        stats = traktV2.shows().stats(show.ids.trakt.toString()).execute().body();
                        break;
                    } catch (IOException e) {
                        // handle exception
                        if (++count == maxTries) try {
                            throw e;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            Log.e("Sync", "couldn't get show stats");
                        }
                    }
                }
                List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.ids.trakt)).list();
                if (sameShows.size() == 0) {
                    List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Recommendation_pos.eq(recommendations.indexOf(show))).list();
                    if (samePos.size() != 0) {
                        samePos.get(0).setRecommendation(false);
                        samePos.get(0).setRecommendation_pos(null);
                        samePos.get(0).update();
                    }
                    ShowEntity newShow = getEntityFromRecommendedShow(show, recommendations.indexOf(show), stats);
                    // add oauth needed columns
                    recommendationsToInsert.add(newShow);
                } else {
                    List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Recommendation_pos.eq(recommendations.indexOf(show))).list();
                    if (samePos.size() != 0) {
                        samePos.get(0).setRecommendation(false);
                        samePos.get(0).setRecommendation_pos(null);
                        samePos.get(0).update();
                    }
                    ShowEntity showEntity = sameShows.get(0);
                    showEntity.setRecommendation(true);
                    showEntity.setRecommendation_pos(recommendations.indexOf(show));
                    if (stats != null) {
                        showEntity.setWatchers(stats.watchers.longValue());
                        showEntity.setPlayers(stats.plays.longValue());
                    }
                    showEntity.update();
                }
            }
            showEntityDao.insertInTx(recommendationsToInsert);
        }
        // trending sync
        List<ShowEntity> trendingToInsert = new ArrayList<>();
        for (TrendingShow show : trendingShows) {
            Stats stats = null;
            count = 0;
            maxTries = 3;
            while(true) {
                try {
                    stats = traktV2.shows().stats(show.show.ids.trakt.toString()).execute().body();
                    break;
                } catch (IOException e) {
                    // handle exception
                    if (++count == maxTries) try {
                        throw e;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e("Sync", "couldn't get show stats");
                    }
                }
            }
            List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.show.ids.trakt)).list();
            if (sameShows.size() == 0) {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trending_pos.eq(trendingShows.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setTrending(false);
                    samePos.get(0).setTrending_pos(null);
                    samePos.get(0).update();
                }
                ShowEntity newShow = getEntityFromTrendingShow(show.show, trendingShows.indexOf(show), stats);
                // add oauth needed columns
                trendingToInsert.add(newShow);
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
                if (stats != null) {
                    showEntity.setWatchers(stats.watchers.longValue());
                    showEntity.setPlayers(stats.plays.longValue());
                }
                showEntity.update();
            }
        }
        showEntityDao.insertInTx(trendingToInsert);
        // most popular sync
        List<ShowEntity> mostPopularToInsert = new ArrayList<>();
        for (Show show : popular) {
            Stats stats = null;
            count = 0;
            maxTries = 3;
            while(true) {
                try {
                    stats = traktV2.shows().stats(show.ids.trakt.toString()).execute().body();
                    break;
                } catch (IOException e) {
                    // handle exception
                    if (++count == maxTries) try {
                        throw e;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e("Sync", "couldn't get show stats");
                    }
                }
            }
            List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.ids.trakt)).list();
            if (sameShows.size() == 0) {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Most_popular_pos.eq(popular.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setMost_popular(false);
                    samePos.get(0).setMost_popular_pos(null);
                    samePos.get(0).update();
                }
                ShowEntity newShow = getEntityFromPopularShow(show, popular.indexOf(show), stats);
                // add oauth needed columns
                mostPopularToInsert.add(newShow);
            } else {
                List<ShowEntity> samePos = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Most_popular_pos.eq(popular.indexOf(show))).list();
                if (samePos.size() != 0) {
                    samePos.get(0).setMost_popular(false);
                    samePos.get(0).setMost_popular_pos(null);
                    samePos.get(0).update();
                }
                ShowEntity showEntity = sameShows.get(0);
                showEntity.setMost_popular(true);
                showEntity.setMost_popular_pos(popular.indexOf(show));
                if (stats != null) {
                    showEntity.setWatchers(stats.watchers.longValue());
                    showEntity.setPlayers(stats.plays.longValue());
                }
                showEntity.update();
            }
        }
        showEntityDao.insertInTx(mostPopularToInsert);
        // remove unused shows
        List<ShowEntity> toDelete = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trending.eq(false),
                ShowEntityDao.Properties.Most_popular.eq(false),
                ShowEntityDao.Properties.Recommendation.eq(false),
                ShowEntityDao.Properties.My_show.eq(false)).list();
        for(ShowEntity deletingShow : toDelete) {
            List<EpisodeEntity> deletingShowEpisodeEntityList = deletingShow.getEpisodeEntityList();
            episodeEntityDao.deleteInTx(deletingShowEpisodeEntityList);
        }
        showEntityDao.deleteInTx(toDelete);
        // episodes sync
        List<EpisodeEntity> episodesToInsert = new ArrayList<>();
        for (ShowEntity showEntity : showEntityDao.loadAll()) {
            // load up all the seasons for show and all episodes for each season
            count = 0;
            maxTries = 3;
            List<Season> seasons;
            while (true) {
                try {
                    seasons = traktV2.seasons().summary(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), Extended.FULL).execute().body();
                    break;
                } catch (IOException e) {
                    // handle exception
                    if (++count == maxTries) try {
                        throw e;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e("sync", "couldn't get seasons");
                    }
                }
            }
            if (seasons != null) {
                showEntity.setSeasons(seasons.size());
                showEntity.update();
                for (Season season : seasons) {
                    // skip the seasons where we have all episodes already
                    /*if (season.aired_episodes == episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(showEntity.getId()), EpisodeEntityDao.Properties.Season.eq(season.number)).count()) {
                        continue;
                    }*/
                    count = 0;
                    maxTries = 3;
                    List<Episode> episodes;
                    while (true) {
                        try {
                            episodes = traktV2.seasons().season(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), season.number, Extended.FULLIMAGES).execute().body();
                            break;
                        } catch (IOException e) {
                            // handle exception
                            if (++count == maxTries) try {
                                throw e;
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                Log.e("sync", "couldn't get episodes");
                            }
                        }
                    }
                    if (episodes != null) {
                        for (Episode episode : episodes) {
                            List<EpisodeEntity> alreadyHave = episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(showEntity.getId()), EpisodeEntityDao.Properties.Season.eq(season.number),
                                    EpisodeEntityDao.Properties.Ep_number.eq(episode.number)).list();
                            if (alreadyHave.size() != 1) {
                                EpisodeEntity newEpisode = getEpisodeEntity(episode, showEntity.getId());
                                // add oauth needed columns
                                episodesToInsert.add(newEpisode);
                            } else {
                                EpisodeEntity existingEpisode = alreadyHave.get(0);
                                // update existingEpisode with new oauth needed columns
                            }
                        }
                    }
                }
            }
        }
        episodeEntityDao.insertInTx(episodesToInsert);
        Log.d("sync", "sync complete");
        EventBus.getDefault().postSticky(new DatabaseUpdatedEvent());
    }

    private static ShowEntity getEntityFromMyShow(BaseShow baseShow, Stats stats) {
        Show show = baseShow.show;
        if (show.genres == null) {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: ", show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, false, null, false, null, true, true);
        } else {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, false, null, false, null, true, true);
        }
    }

    private static ShowEntity getEntityFromRecommendedShow(Show show, int i, Stats stats) {
        if (show.genres == null) {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: ", show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, false, null, true, i, true, false);
        } else {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, false, null, true, i, true, false);
        }
    }

    static EpisodeEntity getEpisodeEntity(Episode episode, Long id) {
        return new EpisodeEntity(null, id, episode.season, episode.title, episode.number, episode.overview,
                false, episode.rating != null ? (int)(episode.rating * 10) : 0, true, episode.images.screenshot.medium);
    }

    private static ShowEntity getEntityFromPopularShow(Show show, int i, Stats stats) {
        if (show.genres == null) {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: ", show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, true, i, false, null, true, false);
        } else {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    false, null, true, i, false, null, true, false);
        }
    }

    private static ShowEntity getEntityFromTrendingShow(Show show, int i, Stats stats) {
        if (show.genres == null) {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: ", show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    true, i, false, null, false, null, true, false);
        } else {
            return new ShowEntity(null, show.ids.trakt, show.title, "genres: " + show.genres.toString().replace("[", "").replace("]", ""), show.overview,
                    0, show.rating != null ? (int)(show.rating * 10) : 0,
                    show.images.poster.thumb, show.images.fanart.medium, show.year, stats.watchers.longValue(), stats.plays.longValue(),
                    true, i, false, null, false, null, true, false);
        }
    }
}
