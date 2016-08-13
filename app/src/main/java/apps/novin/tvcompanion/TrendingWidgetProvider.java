package apps.novin.tvcompanion;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import apps.novin.tvcompanion.db.DaoSession;
import apps.novin.tvcompanion.db.ShowEntity;
import apps.novin.tvcompanion.db.ShowEntityDao;

/**
 * Trending widget shows a random trending show to the user on every update
 */
public class TrendingWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d("widget", "updating");
        DaoSession daoSession = ((App) context.getApplicationContext()).getDaoSession();
        List<ShowEntity> list = daoSession.getShowEntityDao().queryBuilder().where(ShowEntityDao.Properties.Trending.eq(true)).list();
        Set<Integer> used = new HashSet<>(appWidgetIds.length);
        for (int appWidgetId : appWidgetIds) {

            int rand = (int) (Math.random() * 40);
            while (used.contains(rand)) {
                rand = (int) (Math.random() * 40);
            }
            used.add(rand);

            ShowEntity showEntity = list.get(rand);

            boolean bigTablet = context.getString(R.string.screen_type).equals("Big Tablet");

            Intent intent;
            if (bigTablet) {
                intent = new Intent(context, MainActivity.class);
            } else {
                intent = new Intent(context, ShowDetailActivity.class);
            }
            intent.putExtra(ShowDetailActivity.ID_KEY, showEntity.getId());
            intent.setAction(Intent.ACTION_VIEW + showEntity.getId()); // for unique pending intents
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.trending_widget_layout);
            views.setOnClickPendingIntent(R.id.poster, pendingIntent);

            AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context, views, R.id.poster, appWidgetId);

            Glide.with(context.getApplicationContext()) // safer!
                    .load(showEntity.getPoster_url())
                    .asBitmap()
                    .error(R.drawable.ic_close_black)
                    .centerCrop()
                    .into(appWidgetTarget);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
