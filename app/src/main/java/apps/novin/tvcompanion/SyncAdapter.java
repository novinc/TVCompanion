package apps.novin.tvcompanion;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.Stats;
import com.uwetrottmann.trakt5.entities.TrendingShow;
import com.uwetrottmann.trakt5.entities.User;
import com.uwetrottmann.trakt5.entities.Username;
import com.uwetrottmann.trakt5.enums.Extended;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.EpisodeEntity;
import apps.novin.tvcompanion.db.EpisodeEntityDao;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Syncs shows then episodes, also provides static helper methods
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private DaoSession mDaoSession;
    AtomicBoolean canceled;
    static Context context;

    public SyncAdapter(Context context, boolean autoInitialize, DaoSession session) {
        super(context, autoInitialize);
        mDaoSession = session;
        SyncAdapter.context = context;
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        SyncAdapter.context = context;
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit().putBoolean("syncing", false).apply();
        canceled.set(true);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d("sync adapter", "onPerformSync called authority: " + s);
        canceled = new AtomicBoolean(false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        final TraktV2 traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");

        String accessToken = preferences.getString("access_token", null);
        boolean oauth = false;
        if (accessToken != null) {
            traktV2.accessToken(accessToken);
            oauth = true;
        }

        if (!oauth) {
            Log.d("sync", "no oauth, cancelling");
            return;
        }

        Log.d("sync", "oauth true, starting");

        preferences.edit().putBoolean("syncing", true).apply();

        ShowEntityDao showEntityDao = mDaoSession.getShowEntityDao();
        final EpisodeEntityDao episodeEntityDao = mDaoSession.getEpisodeEntityDao();

        if (canceled.get()) {
            return;
        }

        // sync user
        syncUser(traktV2, preferences, getContext());

        if (canceled.get()) {
            return;
        }

        // gets trending and popular shows, along with recommendations and my shows if oauth
        syncShows(preferences, traktV2, showEntityDao, getContext(), true);

        if (canceled.get()) {
            return;
        }

        // remove unused shows
        removeUnusedShows(showEntityDao, episodeEntityDao);

        if (canceled.get()) {
            return;
        }

        // episodes sync
        for (final ShowEntity showEntity : showEntityDao.loadAll()) {
            final List<EpisodeEntity> episodesToInsert = new ArrayList<>();
            getEpisodesFor(showEntity, traktV2, episodeEntityDao, episodesToInsert, false);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    episodeEntityDao.insertInTx(episodesToInsert);
                }
            });
            if (canceled.get()) {
                return;
            }
        }

        Log.d("sync", "episodes sync complete");
        preferences.edit().putBoolean("syncing", false).apply();
        preferences.edit().putLong("sync_time", System.currentTimeMillis()).apply();
        EventBus.getDefault().postSticky(new DatabaseUpdatedEvent());
    }

    private void syncUser(TraktV2 traktV2, SharedPreferences preferences, Context context) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        if (traktV2 == null) {
            traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
            String accessToken = preferences.getString("access_token", null);
            if (accessToken != null) {
                traktV2.accessToken(accessToken);
            }
        }
        User user = null;
        int count = 0;
        int maxTries = 5;
        while(true) {
            try {
                Response<User> execute = traktV2.users().profile(Username.ME, Extended.FULLIMAGES).execute();
                if (execute.isSuccessful()) {
                    user = execute.body();
                } else {
                    if (execute.code() == 401) {
                        // authorization required, supply a valid OAuth access token
                        newToken(traktV2, preferences);
                        execute = traktV2.users().profile(Username.ME, Extended.FULLIMAGES).execute();
                        if (execute.isSuccessful()) {
                            user = execute.body();
                        }
                    }
                }
                break;
            } catch (Exception e) {
                if (++count == maxTries) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        if (user != null) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString("user_photo", user.images.avatar.full);
            edit.putString("user_name", user.username);
            edit.apply();
        }
    }

    private void newToken(TraktV2 traktV2, SharedPreferences preferences) {
        traktV2.refreshToken(preferences.getString("refresh_token", null));
        if (traktV2.refreshToken() != null) {
            AccessToken token = null;
            int count = 0;
            int maxTries = 5;
            while(true) {
                try {
                    token = traktV2.refreshAccessToken().body();
                    break;
                } catch (Exception e) {
                    if (++count == maxTries) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            if (token != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("access_token", token.access_token);
                editor.putString("refresh_token", token.refresh_token);
                editor.apply();
                traktV2.accessToken(token.access_token);
            }
        }
    }

    public static void syncShows(SharedPreferences preferences, TraktV2 traktV2, ShowEntityDao showEntityDao, Context context, boolean fromSync) {
        boolean oauth = false;
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        if (traktV2 == null) {
            traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
            String accessToken = preferences.getString("access_token", null);
            if (accessToken != null) {
                traktV2.accessToken(accessToken);
                oauth = true;
            }
            Log.d("sync", "oauth: " + oauth);
        } else {
            if (traktV2.accessToken() != null) {
                oauth = true;
            }
        }
        if (!fromSync) {
            preferences.edit().putBoolean("syncing", true).apply();
        }
        List<BaseShow> myShows = null;
        List<BaseShow> watchList = null;
        List<Show> recommendations = null;
        List<TrendingShow> trendingShows;
        List<Show> popular;
        if (oauth) {
            myShows = call(traktV2.sync().watchedShows(Extended.FULLIMAGES), "couldn't get my shows");
            watchList = call(traktV2.sync().watchlistShows(Extended.FULLIMAGES), "couldn't get watchlist");
            recommendations = call(traktV2.recommendations().shows(Extended.FULLIMAGES), "couldn't get recommendations");
        }
        trendingShows = call(traktV2.shows().trending(1, 40, Extended.FULLIMAGES), "couldn't get trending shows");
        popular = call(traktV2.shows().popular(1, 40, Extended.FULLIMAGES), "couldn't get popular shows");
        // recommendation and my shows sync
        if (oauth) {
            // my shows
            if (myShows != null) {
                List<ShowEntity> myShowsToInsert = new ArrayList<>();
                for (BaseShow show : myShows) {
                    getStatsForMyShow(show, myShowsToInsert, traktV2, showEntityDao);
                }
                showEntityDao.insertInTx(myShowsToInsert);
            }
            // watchlist shows
            if (watchList != null) {
                List<ShowEntity> watchListToInsert = new ArrayList<>();
                for (BaseShow show : watchList) {
                    getStatsForWatchList(show, watchListToInsert, traktV2, showEntityDao);
                }
                showEntityDao.insertInTx(watchListToInsert);
            }
            // recommendations
            if (recommendations != null) {
                List<ShowEntity> recommendationsToInsert = new ArrayList<>();
                for (Show show : recommendations) {
                    getStatsForRecommendationAndUpdatePositions(show, recommendationsToInsert, recommendations, traktV2, showEntityDao);
                }
                showEntityDao.insertInTx(recommendationsToInsert);
            }
        }
        // trending sync
        if (trendingShows != null) {
            List<ShowEntity> trendingToInsert = new ArrayList<>();
            for (TrendingShow show : trendingShows) {
                getStatsForTrendingAndUpdatePositions(show, trendingShows, trendingToInsert, traktV2, showEntityDao);
            }
            showEntityDao.insertInTx(trendingToInsert);
        }
        // most popular sync
        if (popular != null) {
            List<ShowEntity> mostPopularToInsert = new ArrayList<>();
            for (Show show : popular) {
                getStatsForMostPopularAndUpdatePositions(show, popular, mostPopularToInsert, traktV2, showEntityDao);
            }
            showEntityDao.insertInTx(mostPopularToInsert);
        }
        Log.d("sync", "shows sync complete");
        if (!fromSync) {
            EventBus.getDefault().postSticky(new DatabaseUpdatedEvent());
            preferences.edit().putBoolean("syncing", false).apply();
            preferences.edit().putLong("sync_time", System.currentTimeMillis()).apply();
        }
    }

    private static <T> T call(Call<T> call, String s) {
        int count = 0;
        int maxTries = 5;
        while(true) {
            try {
                return call.execute().body();
            } catch (Exception e) {
                if (++count == maxTries) {
                    e.printStackTrace();
                    Log.d("sync", s);
                    break;
                }
            }
        }
        return null;
    }

    private static void removeUnusedShows(ShowEntityDao showEntityDao, EpisodeEntityDao episodeEntityDao) {
        List<ShowEntity> toDelete = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trending.eq(false),
                ShowEntityDao.Properties.Most_popular.eq(false),
                ShowEntityDao.Properties.Recommendation.eq(false),
                ShowEntityDao.Properties.My_show.eq(false),
                ShowEntityDao.Properties.Watch_list.eq(false)).list();
        for (ShowEntity deletingShow : toDelete) {
            List<EpisodeEntity> deletingShowEpisodeEntityList = deletingShow.getEpisodeEntityList();
            episodeEntityDao.deleteInTx(deletingShowEpisodeEntityList);
        }
        showEntityDao.deleteInTx(toDelete);
    }

    private static void getStatsForMostPopularAndUpdatePositions(Show show, List<Show> popular, List<ShowEntity> mostPopularToInsert, TraktV2 traktV2, ShowEntityDao showEntityDao) {
        Stats stats = call(traktV2.shows().stats(show.ids.trakt.toString()), "couldn't get popular show stats for " + show.title);
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

    private static void getStatsForTrendingAndUpdatePositions(TrendingShow show, List<TrendingShow> trendingShows, List<ShowEntity> trendingToInsert, TraktV2 traktV2, ShowEntityDao showEntityDao) {
        Stats stats = call(traktV2.shows().stats(show.show.ids.trakt.toString()), "couldn't get trending show stats for " + show.show.title);
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

    static void getStatsForRecommendationAndUpdatePositions(Show show, List<ShowEntity> recommendationsToInsert, List<Show> recommendations, TraktV2 traktV2, ShowEntityDao showEntityDao) {
        Stats stats = call(traktV2.shows().stats(show.ids.trakt.toString()), "couldn't get recommendation show stats for " + show.title);
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

    private static void getStatsForMyShow(BaseShow show, List<ShowEntity> myShowsToInsert, TraktV2 traktV2, ShowEntityDao showEntityDao) {
        Stats stats = call(traktV2.shows().stats(show.show.ids.trakt.toString()), "couldn't get my show stats for " + show.show.title);
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
            sameShows.get(0).update();
        }
    }

    private static void getStatsForWatchList(BaseShow show, List<ShowEntity> watchListToInsert, TraktV2 traktV2, ShowEntityDao showEntityDao) {
        Stats stats = call(traktV2.shows().stats(show.show.ids.trakt.toString()), "couldn't get watchlist show stats for " + show.show.title);
        List<ShowEntity> sameShows = showEntityDao.queryBuilder().where(ShowEntityDao.Properties.Trakt_id.eq(show.show.ids.trakt)).list();
        if (sameShows.size() == 0) { // newly watched show not in app's database
            ShowEntity newShow = getEntityFromWatchList(show, stats);
            watchListToInsert.add(newShow);
        } else {
            sameShows.get(0).setWatch_list(true);
            if (stats != null) {
                sameShows.get(0).setWatchers(stats.watchers.longValue());
                sameShows.get(0).setPlayers(stats.plays.longValue());
            }
            sameShows.get(0).update();
        }
    }

    public static void getEpisodesFor(ShowEntity showEntity, TraktV2 traktV2, EpisodeEntityDao episodeEntityDao, List<EpisodeEntity> episodesToInsert, boolean allEpisodes) {
        // load up all the seasons for show and all episodes for each season
        List<Season> seasons = call(traktV2.seasons().summary(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), Extended.FULL), "couldn't get seasons for " + showEntity.getName());
        int episodesForShow;
        if (allEpisodes) {
            episodesForShow = 0;
        } else {
            episodesForShow = showEntity.getEpisodeEntityList().size();
        }
        if (seasons != null) {
            int skipped = 0;
            for (Season season : seasons) {
                // skip the seasons where we have all episodes already
                    /*if (season.aired_episodes == episodeEntityDao.queryBuilder().where(EpisodeEntityDao.Properties.Show_id.eq(showEntity.getId()), EpisodeEntityDao.Properties.Season.eq(season.number)).count()) {
                        continue;
                    }*/
                // skip specials
                if (season.number == 0) {
                    skipped = 1;
                    continue;
                }
                // skip seasons that are not the last and second to last when updating show
                if (season.number < seasons.size() - skipped - 1 && episodesForShow != 0) {
                    continue;
                }
                List<Episode> episodes = call(traktV2.seasons().season(String.format(Locale.ENGLISH, "%d", showEntity.getTrakt_id()), season.number, Extended.FULLIMAGES), "couldn't get season " + season.number + " episodes for " + showEntity.getName());
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
                            // update existingEpisode with new columns
                            EpisodeEntity episodeEntity = getEpisodeEntity(episode, showEntity.getId());
                            existingEpisode.setEp_name(episodeEntity.getEp_name());
                            existingEpisode.setEp_description(episodeEntity.getEp_description());
                            existingEpisode.setPercent_heart(episodeEntity.getPercent_heart());
                            existingEpisode.setPoster_url(episodeEntity.getPoster_url());
                            episodeEntityDao.update(existingEpisode);
                        }
                    }
                }
            }
            showEntity.setSeasons(seasons.size() - skipped);
            showEntity.update();
        }
    }

    private static ShowEntity getEntityFromMyShow(BaseShow baseShow, Stats stats) {
        Show show = baseShow.show;
        return new ShowEntity(null, show.ids.trakt, show.title, context.getString(R.string.genres) + (show.genres == null ? "" : (show.genres.toString().replace("[", "").replace("]", ""))), show.overview,
                0, show.rating != null ? (int) (show.rating * 10) : 0,
                show.images.poster.thumb, show.images.fanart.medium, show.year, stats == null ? 0 : stats.watchers.longValue(), stats == null ? 0 : stats.plays.longValue(),
                false, null, false, null, false, null, true, false);
    }

    private static ShowEntity getEntityFromWatchList(BaseShow baseShow, Stats stats) {
        Show show = baseShow.show;
        return new ShowEntity(null, show.ids.trakt, show.title, context.getString(R.string.genres) + (show.genres == null ? "" : (show.genres.toString().replace("[", "").replace("]", ""))), show.overview,
                0, show.rating != null ? (int) (show.rating * 10) : 0,
                show.images.poster.thumb, show.images.fanart.medium, show.year, stats == null ? 0 : stats.watchers.longValue(), stats == null ? 0 : stats.plays.longValue(),
                false, null, false, null, false, null, false, true);
    }

    private static ShowEntity getEntityFromRecommendedShow(Show show, int i, Stats stats) {
        return new ShowEntity(null, show.ids.trakt, show.title, context.getString(R.string.genres) + (show.genres == null ? "" : (show.genres.toString().replace("[", "").replace("]", ""))), show.overview,
                0, show.rating != null ? (int) (show.rating * 10) : 0,
                show.images.poster.thumb, show.images.fanart.medium, show.year, stats == null ? 0 : stats.watchers.longValue(), stats == null ? 0 : stats.plays.longValue(),
                false, null, false, null, true, i, false, false);
    }

    static EpisodeEntity getEpisodeEntity(Episode episode, Long id) {
        return new EpisodeEntity(null, id, episode.season, episode.title, episode.number, episode.overview,
                episode.rating != null ? (int) (episode.rating * 10) : 0, episode.images.screenshot.full);
    }

    private static ShowEntity getEntityFromPopularShow(Show show, int i, Stats stats) {
        return new ShowEntity(null, show.ids.trakt, show.title, context.getString(R.string.genres) + (show.genres == null ? "" : (show.genres.toString().replace("[", "").replace("]", ""))), show.overview,
                0, show.rating != null ? (int) (show.rating * 10) : 0,
                show.images.poster.thumb, show.images.fanart.medium, show.year, stats == null ? 0 : stats.watchers.longValue(), stats == null ? 0 : stats.plays.longValue(),
                false, null, true, i, false, null, false, false);
    }

    private static ShowEntity getEntityFromTrendingShow(Show show, int i, Stats stats) {
        return new ShowEntity(null, show.ids.trakt, show.title, context.getString(R.string.genres) + (show.genres == null ? "" : (show.genres.toString().replace("[", "").replace("]", ""))), show.overview,
                0, show.rating != null ? (int) (show.rating * 10) : 0,
                show.images.poster.thumb, show.images.fanart.medium, show.year, stats == null ? 0 : stats.watchers.longValue(), stats == null ? 0 : stats.plays.longValue(),
                true, i, false, null, false, null, false, false);
    }
}
