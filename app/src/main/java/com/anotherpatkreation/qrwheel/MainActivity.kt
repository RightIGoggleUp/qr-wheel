package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val io = Executors.newSingleThreadExecutor()
    private var entries: List<Entry> = emptyList()
    private var demo = false
    private lateinit var wheelView: WheelView
    private lateinit var btnSpin: TextView

    private val demoEntries = listOf(
        Entry(name = "Alex"), Entry(name = "Jordan"), Entry(name = "Sam"),
        Entry(name = "Taylor"), Entry(name = "Maya"), Entry(name = "Chris")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wheelView = findViewById(R.id.wheelView)
        btnSpin = findViewById(R.id.btnSpin)

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnEditLink).setOnClickListener { showLinkDialog() }
        findViewById<View>(R.id.tabInfo).setOnClickListener { showTab(0) }
        findViewById<View>(R.id.tabQr).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.tabWheel).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.tabPlayers).setOnClickListener { showTab(3) }

        findViewById<TextView>(R.id.btnAddEntry).setOnClickListener { addEntry() }
        findViewById<TextView>(R.id.btnExport).setOnClickListener { exportPlayers() }

        btnSpin.setOnClickListener {
            if (entries.isEmpty()) {
                Toast.makeText(this, "No names yet - add some on the Info tab", Toast.LENGTH_SHORT).show()
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

        showTab(0)
    }

    override fun onResume() {
        super.onResume()
        renderQr()
        loadLocalEntries()
    }

    private fun showTab(tab: Int) {
        findViewById<View>(R.id.screenInfo).visibility = if (tab == 0) View.VISIBLE else View.GONE
        findViewById<View>(R.id.screenQr).visibility = if (tab == 1) View.VISIBLE else View.GONE
        findViewById<View>(R.id.screenWheel).visibility = if (tab == 2) View.VISIBLE else View.GONE
        findViewById<View>(R.id.screenPlayers).visibility = if (tab == 3) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnEditLink).visibility = if (tab == 1) View.VISIBLE else View.GONE
        styleTab(R.id.tabInfoIcon, R.id.tabInfoLabel, R.id.tabInfoDot, tab == 0)
        styleTab(R.id.tabQrIcon, R.id.tabQrLabel, R.id.tabQrDot, tab == 1)
        styleTab(R.id.tabWheelIcon, R.id.tabWheelLabel, R.id.tabWheelDot, tab == 2)
        styleTab(R.id.tabPlayersIcon, R.id.tabPlayersLabel, R.id.tabPlayersDot, tab == 3)
        if (tab == 2 || tab == 3) loadLocalEntries()
    }

    private fun styleTab(iconId: Int, labelId: Int, dotId: Int, active: Boolean) {
        val color = if (active) Color.WHITE else Color.parseColor("#6E6E7A")
        findViewById<ImageView>(iconId).setColorFilter(color)
        findViewById<TextView>(labelId).setTextColor(color)
        findViewById<View>(dotId).visibility = if (active) View.VISIBLE else View.INVISIBLE
    }

    private fun renderQr() {
        findViewById<QrCardView>(R.id.qrCard).setUrl(Prefs.signupUrl(this))
    }

    private fun showLinkDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_link)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val inp = dialog.findViewById<EditText>(R.id.inpLink)
        inp.setText(Prefs.signupUrl(this))
        inp.setSelectAllOnFocus(true)
        dialog.findViewById<TextView>(R.id.btnSaveLink).setOnClickListener {
            var link = inp.text.toString().trim()
            if (link.isNotBlank() && !link.contains("://")) link = "https://$link"
            if (link.isBlank()) {
                Toast.makeText(this, "Enter a link first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.save(this, link, Prefs.apiBase(this), Prefs.adminToken(this))
            renderQr()
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.btnCancelLink).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addEntry() {
        val nameInp = findViewById<EditText>(R.id.inpName)
        val emailInp = findViewById<EditText>(R.id.inpEmail)
        val name = nameInp.text.toString().trim()
        val email = emailInp.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "That email does not look right", Toast.LENGTH_SHORT).show()
            return
        }
        LocalStore.add(this, name, email)
        nameInp.text.clear()
        emailInp.text.clear()
        nameInp.requestFocus()
        loadLocalEntries()
    }

    private fun loadLocalEntries() {
        val stored = LocalStore.list(this)
        demo = stored.isEmpty()
        setEntries(if (demo) demoEntries else stored, demo)
        renderInfoList(stored)
    }

    private fun setEntries(list: List<Entry>, demo: Boolean) {
        entries = list
        val pool = list.filter { !it.won }
        wheelView.setNames(pool.map { it.name.substringBefore(' ').ifBlank { it.name } })
        val total = list.size
        val countText = if (demo) "No players yet - add them on the Info tab"
            else if (total == 1) "1 player in the drawing" else "$total players in the drawing"
        findViewById<TextView>(R.id.txtPlayerCount).text = countText
    }

    private fun renderInfoList(stored: List<Entry>) {
        val container = findViewById<LinearLayout>(R.id.listEntries)
        container.removeAllViews()
        val pad = (14 * resources.displayMetrics.density).toInt()
        for (e in stored.sortedByDescending { it.id }) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = (if (e.email.isBlank()) e.name else "${e.name} - ${e.email}") + if (e.won) "  🏆" else ""
                setTextColor(Color.parseColor("#F5F5F7"))
                textSize = 17f
                setPadding(0, pad, 0, pad)
            }
            val del = TextView(this).apply {
                text = "Remove"
                setTextColor(Color.parseColor("#E5484D"))
                textSize = 15f
                setPadding(pad, pad, 0, pad)
                setOnClickListener {
                    LocalStore.remove(this@MainActivity, e.id)
                    loadLocalEntries()
                }
            }
            row.addView(label)
            row.addView(del)
            container.addView(row)
        }
    }

    private fun showWinner(w: Entry) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_winner)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtWinName).text = w.name
        dialog.findViewById<TextView>(R.id.txtWinDetails).text =
            if (w.email.isNotBlank()) w.email else "No email on file"

        val notifyBtn = dialog.findViewById<TextView>(R.id.btnNotifyWinner)
        notifyBtn.setOnClickListener {
            notifyBtn.isEnabled = false
            val base = Prefs.apiBase(this)
            if (base.isEmpty()) {
                if (w.email.isBlank()) {
                    Toast.makeText(this, "No email on file for ${w.name}", Toast.LENGTH_LONG).show()
                    notifyBtn.isEnabled = true
                } else {
                    openMailto(w)
                    markWonAndRefresh(w)
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
                                    markWonAndRefresh(w)
                                    dialog.dismiss()
                                }
                                res.reason == "not_connected" -> {
                                    Toast.makeText(this, "Email route not connected yet - opening your mail app instead", Toast.LENGTH_LONG).show()
                                    if (w.email.isNotBlank()) { openMailto(w); markWonAndRefresh(w); dialog.dismiss() } else notifyBtn.isEnabled = true
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

    private fun exportPlayers() {
        val list = LocalStore.list(this)
        if (list.isEmpty()) {
            Toast.makeText(this, "No players yet - add them on the Info tab", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val today = df.format(java.util.Date())
            val sb = StringBuilder("Name,Email,Won\n")
            for (e in list.sortedBy { it.name.lowercase() }) {
                fun esc(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
                sb.append(esc(e.name)).append(',').append(esc(e.email)).append(',').append(if (e.won) "YES" else "").append('\n')
            }
            sb.append("\n").append(list.size).append(if (list.size == 1) " player" else " players").append(" - exported ").append(today).append('\n')
            val dir = java.io.File(cacheDir, "exports").apply { mkdirs() }
            val f = java.io.File(dir, "qr-wheel-players-$today.csv")
            f.writeText(sb.toString())
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "com.anotherpatkreation.qrwheel.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("anotherpatkreation@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "QR Wheel players - $today")
                putExtra(Intent.EXTRA_TEXT, "${list.size} players in the drawing, attached as a spreadsheet. Opens in Excel.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed - " + (e.message ?: "try again"), Toast.LENGTH_LONG).show()
        }
    }

    private fun markWonAndRefresh(w: Entry) {
        if (w.id != 0L) LocalStore.markWon(this, w.id)
        loadLocalEntries()
    }

    private fun openMailto(w: Entry) {
        val subject = Uri.encode(Prefs.winnerSubject(this).replace("{name}", w.name))
        val body = Uri.encode(Prefs.winnerBody(this).replace("{name}", w.name))
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${w.email}?subject=$subject&body=$body")))
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }
}
