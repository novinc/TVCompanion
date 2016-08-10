package apps.novin.tvcompanion;

/**
 * Used by eventbus to notify DB updates
 */
public class DatabaseUpdatedEvent {

    boolean showMessage;

    public DatabaseUpdatedEvent() {
        showMessage = true;
    }

    public DatabaseUpdatedEvent(boolean showMessage) {
        this.showMessage = showMessage;
    }

    public boolean showMessage() {
        return showMessage;
    }
}
