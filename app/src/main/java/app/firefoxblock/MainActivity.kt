package app.firefoxblock

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val hint = TextView(this).apply {
            text = "Firefox hat nur im WLAN Internet. Bei Mobilfunk ist es gesperrt. Andere Apps sind nicht betroffen."
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
        }
        button = Button(this)
        button.setOnClickListener {
            if (FirewallVpnService.running) {
                stopService(Intent(this, FirewallVpnService::class.java))
                refreshLater()
            } else {
                start()
            }
        }

        root.addView(status)
        root.addView(hint)
        root.addView(button)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun start() {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(prepare, 1)
        } else {
            startService(Intent(this, FirewallVpnService::class.java))
            refreshLater()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            startService(Intent(this, FirewallVpnService::class.java))
            refreshLater()
        }
    }

    private fun refreshLater() {
        button.postDelayed({ refresh() }, 400)
    }

    private fun refresh() {
        val on = FirewallVpnService.running
        status.text = if (on) "Sperre AKTIV" else "Sperre aus"
        button.text = if (on) "Ausschalten" else "Einschalten"
    }
}
