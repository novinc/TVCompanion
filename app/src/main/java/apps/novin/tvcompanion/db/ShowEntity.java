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
    /** Not-null value. */
    private String genres;
    /** Not-null value. */
    private String description;
    private int seasons;
    private int percent_heart;
    /** Not-null value. */
    private String poster_url;
    /** Not-null value. */
    private String backdrop_url;
    private int year;
    private long watchers;
    private long players;
    private boolean trending;
    private Integer trending_pos;
    private boolean most_popular;
    private Integer most_popular_pos;
    private boolean synced;
    private boolean my_show;

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

    public ShowEntity(Long id, long trakt_id, String name, String genres, String description, int seasons, int percent_heart, String poster_url, String backdrop_url, int year, long watchers, long players, boolean trending, Integer trending_pos, boolean most_popular, Integer most_popular_pos, boolean synced, boolean my_show) {
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
        this.synced = synced;
        this.my_show = my_show;
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

    /** Not-null value. */
    public String getGenres() {
        return genres;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setGenres(String genres) {
        this.genres = genres;
    }

    /** Not-null value. */
    public String getDescription() {
        return description;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setDescription(String description) {
        this.description = description;
    }

    public int getSeasons() {
        return seasons;
    }

    public void setSeasons(int seasons) {
        this.seasons = seasons;
    }

    public int getPercent_heart() {
        return percent_heart;
    }

    public void setPercent_heart(int percent_heart) {
        this.percent_heart = percent_heart;
    }

    /** Not-null value. */
    public String getPoster_url() {
        return poster_url;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setPoster_url(String poster_url) {
        this.poster_url = poster_url;
    }

    /** Not-null value. */
    public String getBackdrop_url() {
        return backdrop_url;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setBackdrop_url(String backdrop_url) {
        this.backdrop_url = backdrop_url;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getWatchers() {
        return watchers;
    }

    public void setWatchers(long watchers) {
        this.watchers = watchers;
    }

    public long getPlayers() {
        return players;
    }

    public void setPlayers(long players) {
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

    public boolean getSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public boolean getMy_show() {
        return my_show;
    }

    public void setMy_show(boolean my_show) {
        this.my_show = my_show;
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
