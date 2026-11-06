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
Effective: November 6, 2026

FloorDay is built by Another Pat Kreation - anotherpatkreation.com/floorday

WHAT THE APP DOES
FloorDay runs signups and prize drawings at in-person events. Names, email addresses, survey answers, settings, and the winner email template are stored only in the app's private storage on this device.

BADGE SCANNING
- Photos are taken through your device's own camera app. FloorDay never holds or asks for the camera permission.
- Badge photos are processed entirely on this device using Google's ML Kit text recognition. Google states that input images and the text recognized from them are processed fully on-device and are not sent to Google's servers.
- The first time you scan, the phone downloads the recognition model from Google Play services. That is a file download, cached afterward.
- Each scan photo is deleted right after it is read - on success, on failure, and on cancel.

WHAT GOOGLE RECEIVES FROM THE BADGE SCANNER
ML Kit is a Google component built into the app, and FloorDay cannot switch its reporting off. Per Google's own disclosure, the ML Kit SDK sends Google: device information (manufacturer, model, OS version and build), the app's package name and version, a device identifier and per-installation identifiers, performance metrics such as latency, API configuration (image format and resolution), feature version, feature events (such as model downloads and detections), and error codes. Google states it uses this for diagnostics and usage analytics: to measure performance, debug, maintain and improve the APIs, and to detect misuse or abuse. Google states this data is encrypted in transit (HTTPS) and is not transferred to third parties. Google's disclosure does not state a retention period.
This does NOT include badge photos or recognized text - only technical and usage metrics.
Sources: developers.google.com/ml-kit/android-data-disclosure and developers.google.com/ml-kit/terms

NO ACCOUNTS, NO TRACKING
- FloorDay itself has no sign-in, no account, no analytics, no ads, no tracking, and no crash reporting.
- The only third-party data flow is the Google ML Kit diagnostics described above.
- We do not sell or share any data with third parties.

WHEN DATA LEAVES THE DEVICE
Only the ML Kit diagnostics above, and data you explicitly send yourself:
- EMAIL ME THE LIST exports the player list as a spreadsheet through your own email app.
- Winner emails open as drafts in your own email app.
You see exactly what is sent, before it is sent.

NETWORK ACCESS
Internet access is used only for the one-time ML Kit model download and the ML Kit diagnostics described above. FloorDay has no backend, no server, and no account system.

PERMISSIONS
The app requests internet access only. No camera, contacts, location, microphone, or storage permissions are requested.

DATA RETENTION AND DELETION
Players stay on the device until removed. NEW GAME in Settings deletes all players. Uninstalling the app deletes everything it stored. Google does not document a retention period for the ML Kit diagnostics described above.

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
