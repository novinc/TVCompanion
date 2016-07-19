package apps.novin.tvcompanion.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import apps.novin.tvcompanion.db.ShowEntity;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "SHOW_ENTITY".
*/
public class ShowEntityDao extends AbstractDao<ShowEntity, Long> {

    public static final String TABLENAME = "SHOW_ENTITY";

    /**
     * Properties of entity ShowEntity.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Name = new Property(1, String.class, "name", false, "NAME");
        public final static Property Genres = new Property(2, String.class, "genres", false, "GENRES");
        public final static Property Description = new Property(3, String.class, "description", false, "DESCRIPTION");
        public final static Property Seasons = new Property(4, int.class, "seasons", false, "SEASONS");
        public final static Property Percent_heart = new Property(5, int.class, "percent_heart", false, "PERCENT_HEART");
        public final static Property Poster_url = new Property(6, String.class, "poster_url", false, "POSTER_URL");
        public final static Property Backdrop_url = new Property(7, String.class, "backdrop_url", false, "BACKDROP_URL");
        public final static Property Year = new Property(8, int.class, "year", false, "YEAR");
        public final static Property Watchers = new Property(9, long.class, "watchers", false, "WATCHERS");
        public final static Property Players = new Property(10, long.class, "players", false, "PLAYERS");
        public final static Property Trending = new Property(11, boolean.class, "trending", false, "TRENDING");
        public final static Property Trending_pos = new Property(12, Integer.class, "trending_pos", false, "TRENDING_POS");
        public final static Property Most_popular = new Property(13, boolean.class, "most_popular", false, "MOST_POPULAR");
        public final static Property Most_popular_pos = new Property(14, Integer.class, "most_popular_pos", false, "MOST_POPULAR_POS");
        public final static Property Most_played = new Property(15, boolean.class, "most_played", false, "MOST_PLAYED");
        public final static Property Most_played_pos = new Property(16, Integer.class, "most_played_pos", false, "MOST_PLAYED_POS");
        public final static Property Synced = new Property(17, boolean.class, "synced", false, "SYNCED");
    };

    private DaoSession daoSession;


    public ShowEntityDao(DaoConfig config) {
        super(config);
    }
    
    public ShowEntityDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"SHOW_ENTITY\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"NAME\" TEXT NOT NULL ," + // 1: name
                "\"GENRES\" TEXT NOT NULL ," + // 2: genres
                "\"DESCRIPTION\" TEXT NOT NULL ," + // 3: description
                "\"SEASONS\" INTEGER NOT NULL ," + // 4: seasons
                "\"PERCENT_HEART\" INTEGER NOT NULL ," + // 5: percent_heart
                "\"POSTER_URL\" TEXT NOT NULL ," + // 6: poster_url
                "\"BACKDROP_URL\" TEXT NOT NULL ," + // 7: backdrop_url
                "\"YEAR\" INTEGER NOT NULL ," + // 8: year
                "\"WATCHERS\" INTEGER NOT NULL ," + // 9: watchers
                "\"PLAYERS\" INTEGER NOT NULL ," + // 10: players
                "\"TRENDING\" INTEGER NOT NULL ," + // 11: trending
                "\"TRENDING_POS\" INTEGER," + // 12: trending_pos
                "\"MOST_POPULAR\" INTEGER NOT NULL ," + // 13: most_popular
                "\"MOST_POPULAR_POS\" INTEGER," + // 14: most_popular_pos
                "\"MOST_PLAYED\" INTEGER NOT NULL ," + // 15: most_played
                "\"MOST_PLAYED_POS\" INTEGER," + // 16: most_played_pos
                "\"SYNCED\" INTEGER NOT NULL );"); // 17: synced
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"SHOW_ENTITY\"";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, ShowEntity entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getName());
        stmt.bindString(3, entity.getGenres());
        stmt.bindString(4, entity.getDescription());
        stmt.bindLong(5, entity.getSeasons());
        stmt.bindLong(6, entity.getPercent_heart());
        stmt.bindString(7, entity.getPoster_url());
        stmt.bindString(8, entity.getBackdrop_url());
        stmt.bindLong(9, entity.getYear());
        stmt.bindLong(10, entity.getWatchers());
        stmt.bindLong(11, entity.getPlayers());
        stmt.bindLong(12, entity.getTrending() ? 1L: 0L);
 
        Integer trending_pos = entity.getTrending_pos();
        if (trending_pos != null) {
            stmt.bindLong(13, trending_pos);
        }
        stmt.bindLong(14, entity.getMost_popular() ? 1L: 0L);
 
        Integer most_popular_pos = entity.getMost_popular_pos();
        if (most_popular_pos != null) {
            stmt.bindLong(15, most_popular_pos);
        }
        stmt.bindLong(16, entity.getMost_played() ? 1L: 0L);
 
        Integer most_played_pos = entity.getMost_played_pos();
        if (most_played_pos != null) {
            stmt.bindLong(17, most_played_pos);
        }
        stmt.bindLong(18, entity.getSynced() ? 1L: 0L);
    }

    @Override
    protected void attachEntity(ShowEntity entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public ShowEntity readEntity(Cursor cursor, int offset) {
        ShowEntity entity = new ShowEntity( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getString(offset + 1), // name
            cursor.getString(offset + 2), // genres
            cursor.getString(offset + 3), // description
            cursor.getInt(offset + 4), // seasons
            cursor.getInt(offset + 5), // percent_heart
            cursor.getString(offset + 6), // poster_url
            cursor.getString(offset + 7), // backdrop_url
            cursor.getInt(offset + 8), // year
            cursor.getLong(offset + 9), // watchers
            cursor.getLong(offset + 10), // players
            cursor.getShort(offset + 11) != 0, // trending
            cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12), // trending_pos
            cursor.getShort(offset + 13) != 0, // most_popular
            cursor.isNull(offset + 14) ? null : cursor.getInt(offset + 14), // most_popular_pos
            cursor.getShort(offset + 15) != 0, // most_played
            cursor.isNull(offset + 16) ? null : cursor.getInt(offset + 16), // most_played_pos
            cursor.getShort(offset + 17) != 0 // synced
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, ShowEntity entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setName(cursor.getString(offset + 1));
        entity.setGenres(cursor.getString(offset + 2));
        entity.setDescription(cursor.getString(offset + 3));
        entity.setSeasons(cursor.getInt(offset + 4));
        entity.setPercent_heart(cursor.getInt(offset + 5));
        entity.setPoster_url(cursor.getString(offset + 6));
        entity.setBackdrop_url(cursor.getString(offset + 7));
        entity.setYear(cursor.getInt(offset + 8));
        entity.setWatchers(cursor.getLong(offset + 9));
        entity.setPlayers(cursor.getLong(offset + 10));
        entity.setTrending(cursor.getShort(offset + 11) != 0);
        entity.setTrending_pos(cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12));
        entity.setMost_popular(cursor.getShort(offset + 13) != 0);
        entity.setMost_popular_pos(cursor.isNull(offset + 14) ? null : cursor.getInt(offset + 14));
        entity.setMost_played(cursor.getShort(offset + 15) != 0);
        entity.setMost_played_pos(cursor.isNull(offset + 16) ? null : cursor.getInt(offset + 16));
        entity.setSynced(cursor.getShort(offset + 17) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(ShowEntity entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(ShowEntity entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
