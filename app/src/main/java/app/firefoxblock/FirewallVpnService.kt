package app.firefoxblock

import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.widget.Toast

/**
 * Sperrt Firefox, solange kein WLAN verbunden ist (also bei Mobilfunk).
 *
 * Trick: Es wird ein lokaler VPN-Tunnel aufgebaut, durch den NUR Firefox geleitet wird.
 * Der Tunnel liest nichts aus und sendet nichts weiter -> Firefox hat kein Internet.
 * Alle anderen Apps sind nicht betroffen. Sobald WLAN verfügbar ist, wird der Tunnel geschlossen.
 */
class FirewallVpnService : VpnService() {

    companion object {
        @Volatile
        var running = false

        val PACKAGES = listOf(
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.focus",
            "org.mozilla.klar"
        )
    }

    private var tun: ParcelFileDescriptor? = null
    private val wifiNetworks = mutableSetOf<Network>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var cm: ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wifiNetworks.add(network)
            update()
        }

        override fun onLost(network: Network) {
            wifiNetworks.remove(network)
            update()
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        cm = getSystemService(ConnectivityManager::class.java)

        // Bereits verbundene WLANs erfassen (VPNs ausnehmen)
        for (n in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) {
                wifiNetworks.add(n)
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, callback, handler)
        update()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    private fun update() {
        if (wifiNetworks.isEmpty()) block() else unblock()
    }

    private fun installedFirefoxPackages(): List<String> = PACKAGES.filter {
        try {
            packageManager.getPackageInfo(it, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun block() {
        if (tun != null) return

        val targets = installedFirefoxPackages()
        if (targets.isEmpty()) {
            // Sicherheitsnetz: ohne Ziel-App würde der VPN sonst alle Apps betreffen.
            Toast.makeText(this, "Firefox nicht gefunden", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val builder = Builder()
            .setSession("Firefox-Sperre")
            .addAddress("10.255.0.1", 32)
            .addAddress("fd00::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
        targets.forEach { builder.addAllowedApplication(it) }

        val fd = builder.establish()
        if (fd == null) {
            stopSelf()
            return
        }
        tun = fd
    }

    private fun unblock() {
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
    }

    override fun onRevoke() {
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        unblock()
        super.onDestroy()
    }
}
