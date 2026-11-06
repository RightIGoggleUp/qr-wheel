package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val inSignup = findViewById<EditText>(R.id.inSignupUrl)
        val inApi = findViewById<EditText>(R.id.inApiBase)
        val inToken = findViewById<EditText>(R.id.inAdminToken)

        val inWinSubject = findViewById<EditText>(R.id.inWinSubject)
        val inWinBody = findViewById<EditText>(R.id.inWinBody)

        // Option-row editors: per row, an on/off switch, a 1-4 count spinner, 4 label fields.
        val rows = Prefs.optionRows(this).toMutableList()
        val rowSwitches = mutableListOf<Switch>()
        val rowLabels = mutableListOf<List<EditText>>()
        val editor = findViewById<LinearLayout>(R.id.optRowsEditor)
        val density = resources.displayMetrics.density
        rows.forEachIndexed { r, row ->
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val sw = Switch(this).apply {
                text = "Row " + (r + 1)
                setTextColor(Color.parseColor("#F5F5F7"))
                textSize = 16f
                isChecked = row.on
            }
            header.addView(sw, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val hintText = TextView(this).apply {
                text = "blank label = button hidden"
                setTextColor(Color.parseColor("#6E6E7A"))
                textSize = 13f
            }
            header.addView(hintText)
            editor.addView(header)
            val fields = (1..4).map { i ->
                EditText(this).apply {
                    setBackgroundResource(R.drawable.bg_input_dark)
                    setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
                    hint = "Button " + i + " label"
                    setTextColor(Color.parseColor("#F5F5F7"))
                    setHintTextColor(Color.parseColor("#FF6E6E7A"))
                    textSize = 15f
                    setSingleLine(true)
                    setText(row.labels[i - 1])
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (46 * density).toInt())
                    lp.topMargin = (6 * density).toInt()
                    layoutParams = lp
                }
            }
            fields.forEach { editor.addView(it) }
            rowSwitches.add(sw); rowLabels.add(fields)
        }

        inSignup.setText(Prefs.signupUrl(this))
        inApi.setText(Prefs.apiBase(this))
        inToken.setText(Prefs.adminToken(this))
        inWinSubject.setText(Prefs.winnerSubject(this))
        inWinBody.setText(Prefs.winnerBody(this))

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            Prefs.save(
                this,
                inSignup.text.toString(),
                inApi.text.toString(),
                inToken.text.toString()
            )
            Prefs.saveWinnerTemplate(this, inWinSubject.text.toString(), inWinBody.text.toString())
            Prefs.saveOptionRows(this, rows.mapIndexed { r, row ->
                row.copy(
                    on = rowSwitches[r].isChecked,
                    count = rowLabels[r].count { it.text.toString().isNotBlank() }.coerceAtLeast(1),
                    labels = rowLabels[r].map { it.text.toString() }
                )
            })
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<TextView>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        findViewById<TextView>(R.id.btnNewGame).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Start a new game?")
                .setMessage("This erases every player from the drawing - a fresh slate. It cannot be undone.")
                .setPositiveButton("NEW GAME") { _, _ ->
                    LocalStore.clearAll(this)
                    Toast.makeText(this, "Fresh slate - all players cleared", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
