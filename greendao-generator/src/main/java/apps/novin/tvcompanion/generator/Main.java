package apps.novin.tvcompanion.generator;

import java.io.IOException;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

public class Main {
    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "apps.novin.tvcompanion.db");
        Entity show = schema.addEntity("ShowEntity");
        show.implementsSerializable();
        show.addContentProvider();
        show.addIdProperty().autoincrement();
        show.addLongProperty("trakt_id").notNull().unique();
        show.addStringProperty("name").notNull();
        show.addStringProperty("genres").notNull();
        show.addStringProperty("description").notNull();
        show.addIntProperty("seasons").notNull();
        show.addIntProperty("percent_heart").notNull();
        show.addStringProperty("poster_url").notNull();
        show.addStringProperty("backdrop_url").notNull();
        show.addIntProperty("year").notNull();
        show.addLongProperty("watchers").notNull();
        show.addLongProperty("players").notNull();
        show.addBooleanProperty("trending").notNull();
        show.addIntProperty("trending_pos");
        show.addBooleanProperty("most_popular").notNull();
        show.addIntProperty("most_popular_pos");
        show.addBooleanProperty("most_played").notNull();
        show.addIntProperty("most_played_pos");
        show.addBooleanProperty("synced").notNull();

        Entity episode = schema.addEntity("EpisodeEntity");
        episode.implementsSerializable();
        episode.addContentProvider();
        episode.addIdProperty().autoincrement();
        Property epShowId = episode.addLongProperty("show_id").notNull().getProperty();
        episode.addIntProperty("season").notNull();
        episode.addStringProperty("ep_name").notNull();
        episode.addIntProperty("ep_number").notNull();
        episode.addStringProperty("ep_description").notNull();
        episode.addBooleanProperty("watched").notNull();
        episode.addIntProperty("percent_heart").notNull();
        episode.addBooleanProperty("synced").notNull();

        show.addToMany(episode, epShowId);

        DaoGenerator generator = new DaoGenerator();

        generator.generateAll(schema, "app/src/main/java");
    }
}
