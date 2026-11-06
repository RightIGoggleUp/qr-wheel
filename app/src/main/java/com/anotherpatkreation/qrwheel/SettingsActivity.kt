package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
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
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
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
