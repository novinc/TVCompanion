package apps.novin.tvcompanion.db;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.EpisodeEntity;

import apps.novin.tvcompanion.db.ShowEntityDao;
import apps.novin.tvcompanion.db.EpisodeEntityDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig showEntityDaoConfig;
    private final DaoConfig episodeEntityDaoConfig;

    private final ShowEntityDao showEntityDao;
    private final EpisodeEntityDao episodeEntityDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        showEntityDaoConfig = daoConfigMap.get(ShowEntityDao.class).clone();
        showEntityDaoConfig.initIdentityScope(type);

        episodeEntityDaoConfig = daoConfigMap.get(EpisodeEntityDao.class).clone();
        episodeEntityDaoConfig.initIdentityScope(type);

        showEntityDao = new ShowEntityDao(showEntityDaoConfig, this);
        episodeEntityDao = new EpisodeEntityDao(episodeEntityDaoConfig, this);

        registerDao(ShowEntity.class, showEntityDao);
        registerDao(EpisodeEntity.class, episodeEntityDao);
    }
    
    public void clear() {
        showEntityDaoConfig.getIdentityScope().clear();
        episodeEntityDaoConfig.getIdentityScope().clear();
    }

    public ShowEntityDao getShowEntityDao() {
        return showEntityDao;
    }

    public EpisodeEntityDao getEpisodeEntityDao() {
        return episodeEntityDao;
    }

}