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

        inSignup.setText(Prefs.signupUrl(this))
        inApi.setText(Prefs.apiBase(this))
        inToken.setText(Prefs.adminToken(this))

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            Prefs.save(
                this,
                inSignup.text.toString(),
                inApi.text.toString(),
                inToken.text.toString()
            )
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
