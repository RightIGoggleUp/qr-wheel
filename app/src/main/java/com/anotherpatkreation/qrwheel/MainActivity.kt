package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
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

class MainActivity : Activity() {

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
        findViewById<ImageButton>(R.id.btnEditLink).setOnClickListener { showSlotPicker() }
        findViewById<ImageButton>(R.id.btnScanBadge).setOnClickListener { launchScan() }
        findViewById<View>(R.id.tabInfo).setOnClickListener { showTab(0) }
        findViewById<View>(R.id.tabQr).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.tabWheel).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.tabPlayers).setOnClickListener { showTab(3) }

        findViewById<TextView>(R.id.btnAddEntry).setOnClickListener { addEntry() }
        findViewById<TextView>(R.id.btnExport).setOnClickListener { exportPlayers() }
        findViewById<TextView>(R.id.btnEmailParticipants).setOnClickListener { prepareBccEmail() }

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

    private val optSelections = mutableMapOf<Int, MutableSet<Int>>() // row index (0-2) -> selected button indexes (multi-select)
    private var scanPhotoUri: Uri? = null
    private val REQ_SCAN = 41

    override fun onResume() {
        super.onResume()
        renderQrPage()
        renderOptionRows()
        loadLocalEntries()
    }

    private fun renderOptionRows() {
        val rows = Prefs.optionRows(this)
        val containers = listOf(R.id.optRow1, R.id.optRow2, R.id.optRow3, R.id.optRow4)
        val density = resources.displayMetrics.density
        rows.forEachIndexed { r, row ->
            val box = findViewById<LinearLayout>(containers[r])
            box.removeAllViews()
            // Only labels with text become buttons; blanks stay hidden.
            val visible = row.labels.mapIndexedNotNull { i, label -> if (label.isBlank()) null else (i to label) }
            if (!row.on || visible.isEmpty()) { box.visibility = View.GONE; optSelections.remove(r); return@forEachIndexed }
            box.visibility = View.VISIBLE
            visible.forEachIndexed { pos, (i, label) ->
                val selected = optSelections[r]?.contains(i) == true
                val btn = TextView(this).apply {
                    text = if (selected) {
                        val span = android.text.SpannableString("\u25CF  " + label)
                        span.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#6FBA2C")), 0, 1, 0)
                        span
                    } else label
                    textSize = 17f
                    setTextColor(if (selected) Color.WHITE else Color.parseColor("#F5F5F7"))
                    setBackgroundResource(R.drawable.bg_button_dark)
                    gravity = android.view.Gravity.CENTER
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    val m = (4 * density).toInt()
                    lp.setMargins(if (pos == 0) 0 else m, 0, if (pos == visible.size - 1) 0 else m, 0)
                    layoutParams = lp
                    setOnClickListener {
                        val set = optSelections.getOrPut(r) { mutableSetOf() }
                        if (!set.remove(i)) set.add(i)
                        renderOptionRows()
                    }
                }
                box.addView(btn)
            }
        }
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

    private val qrCells = intArrayOf(R.id.qrCell1, R.id.qrCell2, R.id.qrCell3, R.id.qrCell4)
    private val qrViews = intArrayOf(R.id.qrView1, R.id.qrView2, R.id.qrView3, R.id.qrView4)
    private val qrLabels = intArrayOf(R.id.txtQrLabel1, R.id.txtQrLabel2, R.id.txtQrLabel3, R.id.txtQrLabel4)

    private fun renderQrPage() {
        val title = Prefs.qrTitle(this)
        val tv = findViewById<TextView>(R.id.txtQrTitle)
        tv.text = title
        tv.visibility = if (title.isBlank()) View.GONE else View.VISIBLE

        val bg = findViewById<ImageView>(R.id.qrBg)
        val path = Prefs.qrBgPath(this)
        if (path.isNotBlank() && java.io.File(path).exists()) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, opts)
                var sample = 1
                while (maxOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 1600) sample *= 2
                val real = BitmapFactory.Options().apply { inSampleSize = sample }
                bg.setImageBitmap(BitmapFactory.decodeFile(path, real))
            } catch (e: Exception) {
                bg.setImageDrawable(null)
            }
        } else {
            bg.setImageDrawable(null)
        }

        val dm = resources.displayMetrics
        val density = dm.density
        val labelBlock = (54 * density).toInt()      // 8dp margin + 46dp label
        val reserveTop = if (title.isBlank()) (34 * density).toInt() else (110 * density).toInt()
        val reserveBottom = (96 * density).toInt()   // tab bar + screen padding
        val rowH = (dm.heightPixels - reserveTop - reserveBottom) / 2
        val cellW = (dm.widthPixels - 2 * (20 * density).toInt()) / 2
        // Biggest card that fits inside the row WITH its label: no overflow, no clipping.
        val maxCardH = rowH - labelBlock - (8 * density).toInt()
        val maxCardW = cellW - (12 * density).toInt()

        for (i in 0..3) {
            val cell = findViewById<LinearLayout>(qrCells[i])
            val link = Prefs.qrSlotLink(this, i)
            if (link.isBlank()) {
                cell.visibility = View.GONE
                continue
            }
            cell.visibility = View.VISIBLE
            val view = findViewById<QrCardView>(qrViews[i])
            val lp = view.layoutParams
            lp.width = maxCardW; lp.height = maxCardH
            view.layoutParams = lp
            view.setUrl(link)
            val lab = findViewById<TextView>(qrLabels[i])
            val label = Prefs.qrSlotLabel(this, i)
            lab.maxLines = 2
            lab.ellipsize = android.text.TextUtils.TruncateAt.END
            lab.text = label
            lab.visibility = if (label.isBlank()) View.INVISIBLE else View.VISIBLE
            cell.setOnClickListener { showSlotDialog(i) }
        }
    }

    private fun showSlotPicker() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_qr_picker)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val list = dialog.findViewById<LinearLayout>(R.id.listQrSlots)
        val density = resources.displayMetrics.density
        for (i in 0..3) {
            val link = Prefs.qrSlotLink(this, i)
            val label = Prefs.qrSlotLabel(this, i)
            val summary = when {
                label.isNotBlank() -> label
                link.isNotBlank() -> link
                else -> "Empty - tap to set up"
            }
            val row = TextView(this).apply {
                text = "QR CODE " + (i + 1) + "\n" + summary
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setLineSpacing(0f, 1.15f)
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundResource(R.drawable.bg_button_dark)
                val lp2 = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp2.topMargin = (10 * density).toInt()
                layoutParams = lp2
                setOnClickListener {
                    dialog.dismiss()
                    showSlotDialog(i)
                }
            }
            // summary line in dim: overlay via spannable-free approach - second TextView look
            list.addView(row)
        }
        dialog.findViewById<TextView>(R.id.btnCancelPicker).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showSlotDialog(i: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_qr_slot)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtSlotTitle).text = "QR code " + (i + 1)
        val inpLink = dialog.findViewById<EditText>(R.id.inpSlotLink)
        val inpLabel = dialog.findViewById<EditText>(R.id.inpSlotLabel)
        inpLink.setText(Prefs.qrSlotLink(this, i))
        inpLabel.setText(Prefs.qrSlotLabel(this, i))
        inpLink.setSelectAllOnFocus(true)
        // Hard character cap, per Patrick 10:39 AM: max chars = 2 lines x
        // floor(cellW / width of "W"). "W" is the widest Latin glyph at the
        // label's real bold 16sp paint (system font scale included), and cellW
        // is the label's actual wrap width, so ANY string within the cap fits
        // the two rows - labels are never shortened and never ellipsized.
        // StaticLayout stays as a second guard for exotic glyphs wider than W.
        // The cap is shown live under the field. Existing saved labels are
        // never rewritten; an over-cap legacy label shows red in the counter
        // and can only be shortened (insertions reject until within cap).
        val dm2 = resources.displayMetrics
        val cellW = (dm2.widthPixels - 2 * (20 * dm2.density).toInt()) / 2
        val probe = TextView(this).apply { textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD) }
        val paint = probe.paint
        val maxChars = (2 * (cellW / paint.measureText("W")).toInt()).coerceIn(8, 60)
        fun fitsTwoLines(t: CharSequence): Boolean {
            if (t.isEmpty()) return true
            val layout = android.text.StaticLayout.Builder.obtain(t, 0, t.length, paint, cellW).build()
            return layout.lineCount <= 2
        }
        inpLabel.filters = arrayOf(
            android.text.InputFilter.LengthFilter(maxChars),
            android.text.InputFilter { source, sStart, sEnd, dest, dStart, dEnd ->
                val candidate = dest.replaceRange(dStart, dEnd, source.subSequence(sStart, sEnd).toString())
                if (fitsTwoLines(candidate)) null else ""
            }
        )
        val txtLimit = dialog.findViewById<TextView>(R.id.txtLabelLimit)
        fun updateLimit() {
            val n = inpLabel.text.length
            txtLimit.text = "$n / $maxChars characters"
            txtLimit.setTextColor(if (n >= maxChars) Color.parseColor("#E5484D") else Color.parseColor("#9B9BA6"))
        }
        inpLabel.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateLimit() }
        })
        updateLimit()
        dialog.findViewById<TextView>(R.id.btnSaveSlot).setOnClickListener {
            var link = inpLink.text.toString().trim()
            if (link.isNotBlank() && !link.contains("://")) link = "https://$link"
            Prefs.saveQrSlot(this, i, link, inpLabel.text.toString())
            renderQrPage()
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.btnCancelSlot).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun launchScan() {
        val dir = File(cacheDir, "scans").apply { mkdirs() }
        val photo = File(dir, "badge_" + System.currentTimeMillis() + ".jpg")
        val uri = FileProvider.getUriForFile(this, "com.anotherpatkreation.qrwheel.fileprovider", photo)
        scanPhotoUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivityForResult(intent, REQ_SCAN)
        } catch (e: Exception) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SCAN && resultCode == RESULT_OK) {
            val uri = scanPhotoUri ?: return
            val photoPath = File(cacheDir, "scans").listFiles()?.firstOrNull { uri.toString().endsWith(it.name) }
            try {
                val image = InputImage.fromFilePath(this, uri)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { applyScan(it); photoPath?.delete() }
                    .addOnFailureListener {
                        Toast.makeText(this, "Could not read that photo - try again", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Could not read that photo - try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyScan(result: Text) {
        val full = result.text
        Prefs.saveLastScan(this, full)
        var email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(full)?.value ?: ""
        // Name guess: the tallest printed line that is not the email, a phone number, or a URL.
        val noise = setOf("lenovo", "thinkpad", "thinkvision", "thinkstation", "yoga", "motorola", "staff", "exhibitor", "attendee", "visitor", "contractor", "guest", "sponsor", "speaker", "vendor", "press", "vip")
        data class CL(val t: String, val r: android.graphics.Rect?)
        val raw = mutableListOf<CL>()
        for (block in result.textBlocks) for (line in block.lines) {
            val t = line.text.trim()
            if (t.length < 3 || t.length > 40) continue
            if (t.contains('@') || t.contains("http", true) || t.contains("www.", true)) continue
            if (t.count { it.isDigit() } > 1) continue
            if (!t.any { it.isLetter() }) continue
            if (!t.all { it.isLetter() || it == ' ' || it == '.' || it == '-' || it == '\'' }) continue
            if (noise.contains(t.lowercase())) continue
            raw.add(CL(t, line.boundingBox))
        }
        val candidates = mutableListOf<Pair<String, Int>>()
        raw.forEach { candidates.add(it.t to (it.r?.height() ?: 0)) }
        // Badges often stack first/last name on two same-size lines ("Patrick" over "Kea") - merge those.
        for (a in raw) for (b in raw) {
            if (a === b) continue
            val ra = a.r; val rb = b.r
            if (ra == null || rb == null) continue
            val ha = ra.height(); val hb = rb.height()
            if (ha == 0 || hb == 0 || Math.abs(ha - hb) > (ha * 35) / 100) continue
            val gap = rb.top - ra.bottom
            if (gap < -(ha / 5) || gap > (ha * 12) / 10) continue
            if (Math.abs(ra.left - rb.left) > Math.max(ra.width(), rb.width()) * 2 / 5) continue
            candidates.add((a.t + " " + b.t) to (rb.bottom - ra.top))
        }
        val maxH = candidates.maxOfOrNull { it.second } ?: 0
        // Among lines near the biggest print, prefer a two-word line (badges print "First Last").
        val near = candidates.filter { it.second >= (maxH * 7) / 10 }
        var best = (near.firstOrNull { it.first.contains(' ') } ?: near.maxByOrNull { it.second })?.first
        var name = best ?: ""
        if (name.isNotEmpty() && name == name.uppercase() && name.any { it.isLetter() }) {
            name = name.lowercase().split(" ").joinToString(" ") { w ->
                w.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            }
        }
        val nameInp = findViewById<EditText>(R.id.inpName)
        val emailInp = findViewById<EditText>(R.id.inpEmail)
        // Lenovo floor day: no printed email -> first initial + last name @lenovo.com
        if (email.isEmpty() && name.isNotEmpty()) {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            // First initial + LAST name, only with a real first+last pair; single names stay blank for manual entry.
            if (parts.size >= 2) {
                val firstCh = parts.first().firstOrNull { it.isLetter() }?.lowercaseChar()
                val last = parts.last().filter { it.isLetter() }.lowercase()
                if (firstCh != null && last.isNotEmpty()) email = "" + firstCh + last + "@lenovo.com"
            }
        }
        if (name.isEmpty() && email.isEmpty()) {
            Toast.makeText(this, "No text found - try a closer, flatter shot", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isNotEmpty()) nameInp.setText(name)
        if (email.isNotEmpty()) emailInp.setText(email)
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
        val rows = Prefs.optionRows(this)
        val opts = (0..3).map { r ->
            val sel = optSelections[r] ?: emptySet()
            if (rows.getOrNull(r)?.on == true && sel.isNotEmpty())
                sel.sorted().mapNotNull { rows[r].labels.getOrNull(it) }.filter { it.isNotBlank() }.joinToString("; ")
            else ""
        }
        LocalStore.add(this, name, email, opts)
        nameInp.text.clear()
        emailInp.text.clear()
        optSelections.clear()
        renderOptionRows()
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

        dialog.findViewById<TextView>(R.id.btnNotifyWinner).setOnClickListener {
            if (w.email.isBlank()) {
                Toast.makeText(this, "No email on file for ${w.name}", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val subject = Prefs.winnerSubject(this).replace("{name}", w.name)
            val body = Prefs.winnerBody(this).replace("{name}", w.name)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(w.email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            startActivity(Intent.createChooser(intent, "Email ${w.name}"))
            markWonAndRefresh(w)
            dialog.dismiss()
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
            val sb = StringBuilder("Name,Email,Won,Option 1,Option 2,Option 3,Option 4\n")
            for (e in list.sortedBy { it.name.lowercase() }) {
                fun esc(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
                sb.append(esc(e.name)).append(',').append(esc(e.email)).append(',').append(if (e.won) "YES" else "")
                    .append(',').append(esc(e.opt1)).append(',').append(esc(e.opt2)).append(',').append(esc(e.opt3)).append(',').append(esc(e.opt4)).append('\n')
            }
            sb.append("\n").append(list.size).append(if (list.size == 1) " player" else " players").append(" - exported ").append(today).append('\n')
            val dir = java.io.File(cacheDir, "exports").apply { mkdirs() }
            val f = java.io.File(dir, "qr-wheel-players-$today.csv")
            f.writeText(sb.toString())
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "com.anotherpatkreation.qrwheel.fileprovider", f)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("anotherpatkreation@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "FloorDay players - $today")
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

    private fun prepareBccEmail() {
        val organizer = Prefs.organizerEmail(this).trim()
        if (organizer.isBlank()) {
            Toast.makeText(this, "Add your organizer email in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        val emails = LocalStore.list(this)
            .map { it.email.trim() }
            .filter { it.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(it).matches() }
            .map { it.lowercase() }
            .distinct()
        if (emails.isEmpty()) {
            Toast.makeText(this, "No valid participant emails yet", Toast.LENGTH_LONG).show()
            return
        }

        val subject = Prefs.participantSubject(this)
        val html = Prefs.participantHtml(this)
        val formatted = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(organizer))
            putExtra(Intent.EXTRA_BCC, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, formatted)
            putExtra(Intent.EXTRA_HTML_TEXT, html)
        }
        startActivity(Intent.createChooser(intent, "Send participant email with"))
    }

}
