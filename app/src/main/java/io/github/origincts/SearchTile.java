package io.github.origincts;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.TileService;

public final class SearchTile extends TileService {
    @Override public void onClick() {
        super.onClick();
        unlockAndRun(() -> startActivityAndCollapse(PendingIntent.getActivity(this, 0,
                new Intent(this, LaunchActivity.class).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)));
    }
}
