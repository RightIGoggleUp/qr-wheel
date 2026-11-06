package com.anotherpatkreation.qrwheel

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)
        findViewById<TextView>(R.id.privacyBody).text = PRIVACY_TEXT
        findViewById<TextView>(R.id.btnPrivacyDone).setOnClickListener { finish() }
    }

    companion object {
        val PRIVACY_TEXT = """
Effective: November 5, 2026

FloorDay is built by Another Pat Kreation - anotherpatkreation.com/floorday

WHAT THE APP DOES
FloorDay runs signups and prize drawings at in-person events. Names, email addresses, survey answers, settings, and the winner email template are stored only in the app's private storage on this device.

BADGE SCANNING
- Photos are taken through your device's own camera app. FloorDay never holds or asks for the camera permission.
- Badge photos are processed entirely on this device using Google's ML Kit text recognition. Recognition happens on-device: photos and the text pulled from them are not uploaded to us, to Google, or to anyone else.
- The first time you scan, the phone downloads the recognition model from Google Play services. That is a one-time file download - no personal data is sent - and it is cached afterward.
- Each scan photo is deleted right after it is read.

NO ACCOUNTS, NO TRACKING
- No sign-in, no account, no analytics, no ads, no tracking, no crash reporting.
- No data is sold or shared with third parties.

WHEN DATA LEAVES THE DEVICE
Only when you explicitly send it:
- EMAIL ME THE LIST exports the player list as a spreadsheet through your own email app.
- Winner emails open as drafts in your own email app.
You see exactly what is sent, before it is sent.

NETWORK ACCESS
Internet access is used for the one-time model download and for an optional organizer backend. The backend is configured by the organizer; when it is enabled, signup fields entered on the Info screen go to that service, and this policy will be updated at anotherpatkreation.com/floorday.

PERMISSIONS
The app requests internet access only. No camera, contacts, location, microphone, or storage permissions are requested.

DATA RETENTION AND DELETION
Players stay on the device until removed. NEW GAME in Settings deletes all players. Uninstalling the app deletes everything it stored.

CHILDREN
FloorDay is a tool for event organizers and is not directed at children.

CHANGES
Any change to this policy ships in the app and is posted at anotherpatkreation.com/floorday.

CONTACT
contactme@anotherpatkreation.com

LEGAL
FloorDay is provided as-is for event organizers. Information collected from entrants belongs to the organizer's event; the organizer is responsible for how it is used.
        """.trimIndent()
    }
}
