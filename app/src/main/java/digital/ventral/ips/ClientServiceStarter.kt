package digital.ventral.ips

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives events to start the ClientService
 *
 * The ServerService is started only on-demand when there's something to share and isn't killed
 * by Android to save battery because it's a foreground service keeping itself alive with a sticky
 * notification.
 *
 * The ClientService on the other hand is a simple background service which should silently wait
 * for a ServerService to become available with new shared items. Ideally we'd want the service to
 * always be running in the background, waiting for relevant runtime-registered events (eg. user
 * switch) triggering a check. But that won't always be the case as Android has the tendency to
 * kill Services it deems unnecessarily consuming battery power.
 *
 * - ACTION_BOOT_COMPLETED
 *   Broadcast once, after the user has finished booting. Requires a permission of the same name.
 * - ACTION_LOCKED_BOOT_COMPLETED
 *   Broadcast once, after the user has finished booting, but while still in the "locked" state.
 *   Also requires the ACTION_BOOT_COMPLETED permission.
 * - ACTION_DREAMING_STOPPED
 *   Sent after the system stops "dreaming", ie. taken out of docking station where it showed
 *   an interactive screensaver.
 * - ACTION_BATTERY_OKAY
 *   Sent after battery is charged after being low.
 * - ACTION_POWER_CONNECTED
 *   External power has been connected to the device.
 * - ACTION_POWER_DISCONNECTED
 *   External power has been removed from the device.
 * - ACTION_MY_PACKAGE_REPLACED
 *   New version of this application has been installed over an existing one.
 * - ACTION_MY_PACKAGE_UNSUSPENDED
 *   This application is no longer suspended and may create notifications again.
 */
class ClientServiceStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_DREAMING_STOPPED,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_MY_PACKAGE_UNSUSPENDED -> {
                android.util.Log.d("ClientServiceStarter", "Manifest registered broadcast of ${intent.action} received, start ClientService")
                context.startService(Intent(context, ClientService::class.java))
            }
        }
    }
}