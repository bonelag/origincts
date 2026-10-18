package io.github.origincts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

/** Wait for the trampoline to leave the screen, rather than sleeping a fixed 700 ms. */
public final class LaunchActivity extends Activity {
    private boolean requested;
    private boolean resumed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
    }
    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        finishAndRemoveTask();
    }
    @Override protected void onStop() {
        super.onStop();
        if (!resumed || requested) return;
        requested = true;
        Context context = getApplicationContext();
        Intent request = new Intent(BridgeContract.ACTION).setPackage(BridgeContract.SYSTEM_UI)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("requested_at", SystemClock.uptimeMillis())
                .putExtra("gemini", getIntent().getBooleanExtra("gemini", false));
        try {
            context.sendOrderedBroadcast(request, null, new BroadcastReceiver() {
                @Override public void onReceive(Context ignored, Intent intent) {
                    if (getResultCode() != RESULT_OK) {
                        String error = getResultData();
                        Toast.makeText(context, error == null ? "CTS is not ready." : error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            }, null, RESULT_CANCELED,
                    "SystemUI is not connected. Check the LSPosed scope and logs.", null);
        } catch (Exception failure) {
            Toast.makeText(context, "Could not request CTS: " + failure.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
