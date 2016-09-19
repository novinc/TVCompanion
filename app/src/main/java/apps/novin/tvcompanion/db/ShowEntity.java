package apps.novin.tvcompanion.db;

import java.util.List;
import apps.novin.tvcompanion.db.DaoSession;
import de.greenrobot.dao.DaoException;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "SHOW_ENTITY".
 */
public class ShowEntity implements java.io.Serializable {

    private Long id;
    private long trakt_id;
    /** Not-null value. */
    private String name;
    private String genres;
    private String description;
    private Integer seasons;
    private Integer percent_heart;
    private String poster_url;
    private String backdrop_url;
    private Integer year;
    private Long watchers;
    private Long players;
    private boolean trending;
    private Integer trending_pos;
    private boolean most_popular;
    private Integer most_popular_pos;
    private boolean recommendation;
    private Integer recommendation_pos;
    private boolean my_show;
    private boolean watch_list;

    /** Used to resolve relations */
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    private transient ShowEntityDao myDao;

    private List<EpisodeEntity> episodeEntityList;

    public ShowEntity() {
    }

    public ShowEntity(Long id) {
        this.id = id;
    }

    public ShowEntity(Long id, long trakt_id, String name, String genres, String description, Integer seasons, Integer percent_heart, String poster_url, String backdrop_url, Integer year, Long watchers, Long players, boolean trending, Integer trending_pos, boolean most_popular, Integer most_popular_pos, boolean recommendation, Integer recommendation_pos, boolean my_show, boolean watch_list) {
        this.id = id;
        this.trakt_id = trakt_id;
        this.name = name;
        this.genres = genres;
        this.description = description;
        this.seasons = seasons;
        this.percent_heart = percent_heart;
        this.poster_url = poster_url;
        this.backdrop_url = backdrop_url;
        this.year = year;
        this.watchers = watchers;
        this.players = players;
        this.trending = trending;
        this.trending_pos = trending_pos;
        this.most_popular = most_popular;
        this.most_popular_pos = most_popular_pos;
        this.recommendation = recommendation;
        this.recommendation_pos = recommendation_pos;
        this.my_show = my_show;
        this.watch_list = watch_list;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getShowEntityDao() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTrakt_id() {
        return trakt_id;
    }

    public void setTrakt_id(long trakt_id) {
        this.trakt_id = trakt_id;
    }

    /** Not-null value. */
    public String getName() {
        return name;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setName(String name) {
        this.name = name;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSeasons() {
        return seasons;
    }

    public void setSeasons(Integer seasons) {
        this.seasons = seasons;
    }

    public Integer getPercent_heart() {
        return percent_heart;
    }

    public void setPercent_heart(Integer percent_heart) {
        this.percent_heart = percent_heart;
    }

    public String getPoster_url() {
        return poster_url;
    }

    public void setPoster_url(String poster_url) {
        this.poster_url = poster_url;
    }

    public String getBackdrop_url() {
        return backdrop_url;
    }

    public void setBackdrop_url(String backdrop_url) {
        this.backdrop_url = backdrop_url;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getWatchers() {
        return watchers;
    }

    public void setWatchers(Long watchers) {
        this.watchers = watchers;
    }

    public Long getPlayers() {
        return players;
    }

    public void setPlayers(Long players) {
        this.players = players;
    }

    public boolean getTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    public Integer getTrending_pos() {
        return trending_pos;
    }

    public void setTrending_pos(Integer trending_pos) {
        this.trending_pos = trending_pos;
    }

    public boolean getMost_popular() {
        return most_popular;
    }

    public void setMost_popular(boolean most_popular) {
        this.most_popular = most_popular;
    }

    public Integer getMost_popular_pos() {
        return most_popular_pos;
    }

    public void setMost_popular_pos(Integer most_popular_pos) {
        this.most_popular_pos = most_popular_pos;
    }

    public boolean getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(boolean recommendation) {
        this.recommendation = recommendation;
    }

    public Integer getRecommendation_pos() {
        return recommendation_pos;
    }

    public void setRecommendation_pos(Integer recommendation_pos) {
        this.recommendation_pos = recommendation_pos;
    }

    public boolean getMy_show() {
        return my_show;
    }

    public void setMy_show(boolean my_show) {
        this.my_show = my_show;
    }

    public boolean getWatch_list() {
        return watch_list;
    }

    public void setWatch_list(boolean watch_list) {
        this.watch_list = watch_list;
    }

    /** To-many relationship, resolved on first access (and after reset). Changes to to-many relations are not persisted, make changes to the target entity. */
    public List<EpisodeEntity> getEpisodeEntityList() {
        if (episodeEntityList == null) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            EpisodeEntityDao targetDao = daoSession.getEpisodeEntityDao();
            List<EpisodeEntity> episodeEntityListNew = targetDao._queryShowEntity_EpisodeEntityList(id);
            synchronized (this) {
                if(episodeEntityList == null) {
                    episodeEntityList = episodeEntityListNew;
                }
            }
        }
        return episodeEntityList;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    public synchronized void resetEpisodeEntityList() {
        episodeEntityList = null;
    }

    /** Convenient call for {@link AbstractDao#delete(Object)}. Entity must attached to an entity context. */
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.delete(this);
    }

    /** Convenient call for {@link AbstractDao#update(Object)}. Entity must attached to an entity context. */
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.update(this);
    }

    /** Convenient call for {@link AbstractDao#refresh(Object)}. Entity must attached to an entity context. */
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.refresh(this);
    }

}
