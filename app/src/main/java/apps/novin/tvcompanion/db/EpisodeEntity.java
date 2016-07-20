package apps.novin.tvcompanion.db;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "EPISODE_ENTITY".
 */
public class EpisodeEntity implements java.io.Serializable {

    private Long id;
    private long show_id;
    private int season;
    private String ep_name;
    private int ep_number;
    private String ep_description;
    private boolean watched;
    private int percent_heart;
    private boolean synced;
    private String poster_url;

    public EpisodeEntity() {
    }

    public EpisodeEntity(Long id) {
        this.id = id;
    }

    public EpisodeEntity(Long id, long show_id, int season, String ep_name, int ep_number, String ep_description, boolean watched, int percent_heart, boolean synced, String poster_url) {
        this.id = id;
        this.show_id = show_id;
        this.season = season;
        this.ep_name = ep_name;
        this.ep_number = ep_number;
        this.ep_description = ep_description;
        this.watched = watched;
        this.percent_heart = percent_heart;
        this.synced = synced;
        this.poster_url = poster_url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getShow_id() {
        return show_id;
    }

    public void setShow_id(long show_id) {
        this.show_id = show_id;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getEp_name() {
        return ep_name;
    }

    public void setEp_name(String ep_name) {
        this.ep_name = ep_name;
    }

    public int getEp_number() {
        return ep_number;
    }

    public void setEp_number(int ep_number) {
        this.ep_number = ep_number;
    }

    public String getEp_description() {
        return ep_description;
    }

    public void setEp_description(String ep_description) {
        this.ep_description = ep_description;
    }

    public boolean getWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public int getPercent_heart() {
        return percent_heart;
    }

    public void setPercent_heart(int percent_heart) {
        this.percent_heart = percent_heart;
    }

    public boolean getSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public String getPoster_url() {
        return poster_url;
    }

    public void setPoster_url(String poster_url) {
        this.poster_url = poster_url;
    }

}
