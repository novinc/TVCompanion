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
        show.addStringProperty("genres");
        show.addStringProperty("description");
        show.addIntProperty("seasons");
        show.addIntProperty("percent_heart");
        show.addStringProperty("poster_url");
        show.addStringProperty("backdrop_url");
        show.addIntProperty("year");
        show.addLongProperty("watchers");
        show.addLongProperty("players");
        show.addBooleanProperty("trending").notNull();
        show.addIntProperty("trending_pos").unique();
        show.addBooleanProperty("most_popular").notNull();
        show.addIntProperty("most_popular_pos").unique();
        show.addBooleanProperty("recommendation").notNull();
        show.addIntProperty("recommendation_pos").unique();
        show.addBooleanProperty("synced").notNull();
        show.addBooleanProperty("my_show").notNull();

        Entity episode = schema.addEntity("EpisodeEntity");
        episode.implementsSerializable();
        episode.addContentProvider();
        episode.addIdProperty().autoincrement();
        Property epShowId = episode.addLongProperty("show_id").notNull().getProperty();
        episode.addIntProperty("season").notNull();
        episode.addStringProperty("ep_name");
        episode.addIntProperty("ep_number").notNull();
        episode.addStringProperty("ep_description");
        episode.addBooleanProperty("watched").notNull();
        episode.addIntProperty("percent_heart");
        episode.addBooleanProperty("synced").notNull();
        episode.addStringProperty("poster_url");

        show.addToMany(episode, epShowId);

        DaoGenerator generator = new DaoGenerator();

        generator.generateAll(schema, "app/src/main/java");
    }
}
