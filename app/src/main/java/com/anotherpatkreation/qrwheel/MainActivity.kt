package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val io = Executors.newSingleThreadExecutor()
    private var entries: List<Entry> = emptyList()
    private lateinit var wheelView: WheelView
    private lateinit var txtCount: TextView
    private lateinit var btnSpin: TextView

    private val demoEntries = listOf(
        Entry(name = "Alex"), Entry(name = "Jordan"), Entry(name = "Sam"),
        Entry(name = "Taylor"), Entry(name = "Maya"), Entry(name = "Chris")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wheelView = findViewById(R.id.wheelView)
        txtCount = findViewById(R.id.txtCount)
        btnSpin = findViewById(R.id.btnSpin)

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.tabQr).setOnClickListener { showTab(true) }
        findViewById<View>(R.id.tabWheel).setOnClickListener { showTab(false) }
        findViewById<View>(R.id.btnRefresh).setOnClickListener { loadEntries() }

        btnSpin.setOnClickListener {
            if (entries.isEmpty()) {
                Toast.makeText(this, "No signups yet", Toast.LENGTH_SHORT).show()
            } else {
                btnSpin.isEnabled = false
                wheelView.spin()
            }
        }

        wheelView.onWinner = winnerCb@{ idx ->
            btnSpin.isEnabled = true
            val winner = entries.getOrNull(idx) ?: return@winnerCb
            showWinner(winner)
        }

        showTab(true)
    }

    override fun onResume() {
        super.onResume()
        renderQr()
        loadEntries()
    }

    private fun showTab(qr: Boolean) {
        findViewById<View>(R.id.screenQr).visibility = if (qr) View.VISIBLE else View.GONE
        findViewById<View>(R.id.screenWheel).visibility = if (qr) View.GONE else View.VISIBLE
        styleTab(R.id.tabQrIcon, R.id.tabQrLabel, R.id.tabQrDot, qr)
        styleTab(R.id.tabWheelIcon, R.id.tabWheelLabel, R.id.tabWheelDot, !qr)
        if (!qr) loadEntries()
    }

    private fun styleTab(iconId: Int, labelId: Int, dotId: Int, active: Boolean) {
        val color = if (active) Color.WHITE else Color.parseColor("#6E6E7A")
        findViewById<ImageView>(iconId).setColorFilter(color)
        findViewById<TextView>(labelId).setTextColor(color)
        findViewById<View>(dotId).visibility = if (active) View.VISIBLE else View.INVISIBLE
    }

    private fun renderQr() {
        val url = Prefs.signupUrl(this)
        findViewById<TextView>(R.id.txtQrUrl).text =
            url.removePrefix("https://").removePrefix("http://")
        findViewById<QrCardView>(R.id.qrCard).setUrl(url)
    }

    private fun loadEntries() {
        val base = Prefs.apiBase(this)
        if (base.isEmpty()) {
            setEntries(demoEntries, demo = true)
            return
        }
        txtCount.text = "Loading signups…"
        io.execute {
            try {
                val list = Api.fetchEntries(base, Prefs.adminToken(this))
                runOnUiThread { setEntries(list, demo = false) }
            } catch (e: Exception) {
                runOnUiThread {
                    txtCount.text = "Could not reach backend - check Settings"
                }
            }
        }
    }

    private fun setEntries(list: List<Entry>, demo: Boolean) {
        entries = list
        wheelView.setNames(list.map { it.name.substringBefore(' ').ifBlank { it.name } })
        val n = list.size
        val label = if (n == 1) "1 person signed up" else "$n people signed up"
        txtCount.text = if (demo) "$label (demo)" else label
    }

    private fun showWinner(w: Entry) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_winner)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtWinName).text = w.name
        val details = buildString {
            if (w.email.isNotBlank()) append(w.email).append('\n')
            if (w.sells.isNotBlank()) append("Sells: ").append(w.sells)
        }.trim()
        dialog.findViewById<TextView>(R.id.txtWinDetails).text =
            details.ifBlank { "No contact details on file" }

        val notifyBtn = dialog.findViewById<TextView>(R.id.btnNotifyWinner)
        notifyBtn.setOnClickListener {
            notifyBtn.isEnabled = false
            val base = Prefs.apiBase(this)
            if (base.isEmpty()) {
                // demo mode: hand a pre-written email to the mail app instead
                val to = w.email
                if (to.isBlank()) {
                    Toast.makeText(this, "Connect the backend in Settings to notify winners", Toast.LENGTH_LONG).show()
                    notifyBtn.isEnabled = true
                } else {
                    openMailto(w)
                    dialog.dismiss()
                }
            } else {
                io.execute {
                    try {
                        val res = Api.notifyWinner(base, Prefs.adminToken(this), w)
                        runOnUiThread {
                            when {
                                res.emailed -> {
                                    Toast.makeText(this, "Email sent to ${w.name}", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                                res.reason == "not_connected" -> {
                                    Toast.makeText(this, "Outlook not connected yet - opening your mail app instead", Toast.LENGTH_LONG).show()
                                    openMailto(w)
                                    dialog.dismiss()
                                }
                                else -> {
                                    Toast.makeText(this, "Could not send (${res.reason}) - try again", Toast.LENGTH_LONG).show()
                                    notifyBtn.isEnabled = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "Could not reach backend - try again", Toast.LENGTH_LONG).show()
                            notifyBtn.isEnabled = true
                        }
                    }
                }
            }
        }
        dialog.findViewById<TextView>(R.id.btnCloseWinner).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun openMailto(w: Entry) {
        val subject = Uri.encode("You won the prize drawing!")
        val body = Uri.encode(
            "Hi ${w.name}!\n\nCongratulations - you won today's prize drawing. " +
                "Stop by our table to pick up your prize.\n\nThanks for entering!"
        )
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${w.email}?subject=$subject&body=$body")))
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }
}
