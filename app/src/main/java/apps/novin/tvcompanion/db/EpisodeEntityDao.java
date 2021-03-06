package apps.novin.tvcompanion.db;

import java.util.List;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;

import apps.novin.tvcompanion.db.EpisodeEntity;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "EPISODE_ENTITY".
*/
public class EpisodeEntityDao extends AbstractDao<EpisodeEntity, Long> {

    public static final String TABLENAME = "EPISODE_ENTITY";

    /**
     * Properties of entity EpisodeEntity.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Show_id = new Property(1, long.class, "show_id", false, "SHOW_ID");
        public final static Property Season = new Property(2, int.class, "season", false, "SEASON");
        public final static Property Ep_name = new Property(3, String.class, "ep_name", false, "EP_NAME");
        public final static Property Ep_number = new Property(4, int.class, "ep_number", false, "EP_NUMBER");
        public final static Property Ep_description = new Property(5, String.class, "ep_description", false, "EP_DESCRIPTION");
        public final static Property Percent_heart = new Property(6, Integer.class, "percent_heart", false, "PERCENT_HEART");
        public final static Property Poster_url = new Property(7, String.class, "poster_url", false, "POSTER_URL");
    };

    private Query<EpisodeEntity> showEntity_EpisodeEntityListQuery;

    public EpisodeEntityDao(DaoConfig config) {
        super(config);
    }
    
    public EpisodeEntityDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"EPISODE_ENTITY\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"SHOW_ID\" INTEGER NOT NULL ," + // 1: show_id
                "\"SEASON\" INTEGER NOT NULL ," + // 2: season
                "\"EP_NAME\" TEXT," + // 3: ep_name
                "\"EP_NUMBER\" INTEGER NOT NULL ," + // 4: ep_number
                "\"EP_DESCRIPTION\" TEXT," + // 5: ep_description
                "\"PERCENT_HEART\" INTEGER," + // 6: percent_heart
                "\"POSTER_URL\" TEXT);"); // 7: poster_url
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"EPISODE_ENTITY\"";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, EpisodeEntity entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getShow_id());
        stmt.bindLong(3, entity.getSeason());
 
        String ep_name = entity.getEp_name();
        if (ep_name != null) {
            stmt.bindString(4, ep_name);
        }
        stmt.bindLong(5, entity.getEp_number());
 
        String ep_description = entity.getEp_description();
        if (ep_description != null) {
            stmt.bindString(6, ep_description);
        }
 
        Integer percent_heart = entity.getPercent_heart();
        if (percent_heart != null) {
            stmt.bindLong(7, percent_heart);
        }
 
        String poster_url = entity.getPoster_url();
        if (poster_url != null) {
            stmt.bindString(8, poster_url);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public EpisodeEntity readEntity(Cursor cursor, int offset) {
        EpisodeEntity entity = new EpisodeEntity( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getLong(offset + 1), // show_id
            cursor.getInt(offset + 2), // season
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // ep_name
            cursor.getInt(offset + 4), // ep_number
            cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5), // ep_description
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6), // percent_heart
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7) // poster_url
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, EpisodeEntity entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setShow_id(cursor.getLong(offset + 1));
        entity.setSeason(cursor.getInt(offset + 2));
        entity.setEp_name(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setEp_number(cursor.getInt(offset + 4));
        entity.setEp_description(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setPercent_heart(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setPoster_url(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(EpisodeEntity entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(EpisodeEntity entity) {
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
    
    /** Internal query to resolve the "episodeEntityList" to-many relationship of ShowEntity. */
    public List<EpisodeEntity> _queryShowEntity_EpisodeEntityList(long show_id) {
        synchronized (this) {
            if (showEntity_EpisodeEntityListQuery == null) {
                QueryBuilder<EpisodeEntity> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.Show_id.eq(null));
                showEntity_EpisodeEntityListQuery = queryBuilder.build();
            }
        }
        Query<EpisodeEntity> query = showEntity_EpisodeEntityListQuery.forCurrentThread();
        query.setParameter(0, show_id);
        return query.list();
    }

}
