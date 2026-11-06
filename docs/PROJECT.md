# QR Wheel - project record

Living document for Part 1 of the QR signup + prize wheel project.
Dates are America/New_York.

## Charter (Part 1)

- **Goal:** give event attendees a one-scan way to sign up, and give the
  presenter a fair, fun way to pick a prize winner - with automatic,
  templated email follow-up.
- **Scope in:** signup web page, backend storage + Excel export, Android
  presenter app (QR display + spinning wheel), winner notification by email.
- **Scope out (Part 2):** multi-user/self-serve version for other event
  hosts, automated SMS, paid tiers of any service.
- **Constraints:** free tiers only; no servers to maintain; mockups approved
  by the sponsor are the design of record.

## Decision log
### 2026-11-05 - v0.1.0-test2: branded QR + fixed layouts
- Decision: QR style = "Round" with full-width official Lenovo red box ABOVE the code (Patrick picked from mockups). Full-width band THROUGH the code tested unscannable (OpenCV + ZXing, 5 heights) - documented as a hard limit. Centered badge max scan-safe size = 34% of card width.
- Decision: Lenovo branding only; ISG left out (no ISG logo exists - letters only, and Patrick opted to drop it entirely). Logo asset = official public corporate logo file.
- EC level H mandatory for styled QR; recipe decode-verified with ZXing + OpenCV at full/phone sizes before shipping.
- Layouts now fixed/no-scroll, large-and-loud per Patrick's direction.
- Pending: on-device camera verification of the styled QR on Patrick's razr.


| Date (2026) | Decision | Rationale |
|---|---|---|
| Nov 5 | Email for all notifications; SMS dropped | Free vs. per-text carrier cost |
| Nov 5 | Phone number field removed from the form | Sponsor decision; email-only contact |
| Nov 5 | Temporary Cloudflare subdomain first, custom domain later | anotherpatkreation.com mid-transfer; URL is a config value, not code |
| Nov 5 | Cloudflare Workers + D1 (SQLite) backend | Free tier, no server maintenance, sponsor already consolidating on Cloudflare |
| Nov 5 | Two switchable sender routes: work Outlook (Microsoft Graph) / Gmail (Brevo free tier) | Work IT may block app sending; sponsor asked for both options |
| Nov 5 | Winner notification requires a tap ("Notify the winner?") | Human confirmation before anyone is emailed |
| Nov 5 | Public GitHub repo | Sponsor had trouble downloading from private repos |
| Nov 5 | Demo mode in app when no backend configured | Testable before backend deployment |

## Open items

- Cloudflare deployment (needs sponsor's 2FA code at deploy time)
- Entra app registration for the Outlook route (sponsor or work IT)
- Brevo API key for the Gmail route (sponsor)
- Email template copy approval (placeholders in admin page for now)
- Custom domain attach once the transfer completes

## 2026-11-05 (test4 additions)
- Winner email template: Patrick's own Outlook .msg baked in (subject blank in the file; body = "SSG Floor Day November 11th, 2026" + his lemonade quote + full Lenovo signature). `{name}` placeholder inserts the winner's first name. Editable in Settings (subject + body) - in-place, two-tap editing is his validated pattern (pencil edit on QR page: "Great job").
- Default QR link = his Lenovo Highspot item URL; still user-changeable via pencil.
- QR card layout: shrunk to 97% and biased upward (+22dp bottom padding on QR screen) after he reported the white card clipped at the bottom of his razr's screen.
- Product goal recorded: Play Store release. Keep store-friendly: current targetSdk, signing compatible with Play App Signing, no unused permissions, no hardcoded environment values.
- Four tabs now: Info (entry form only - attendee names never show on the entry screen, his privacy call), QR Code, Prize Wheel, Players (the collected list with per-row Remove). "Players" is his picked name for the list tab. Wheel still draws from the same on-device list.
- Winner rule (his): once a winner's notification goes out (email-app handoff or backend send), that name comes off the wheel until a new game. Players tab marks winners with "WON". Settings has NEW GAME - PUT WINNERS BACK to reset the pool; entries are never deleted by winning.
- Email route: Patrick picked Gmail/Brevo with sender anotherpatkreation@gmail.com (no personal addresses). Brevo free tier (300 emails/day, no card, no DNS changes - single-sender verification only). His .msg HTML is now the backend winner template (tpl_winner_html, seeded by web/seed_winner_template.sh at deploy); signature images extracted from the .msg are served at /img/*. Brevo + Graph both send full HTML; the plain-text template remains the fallback. The app's mailto handoff stays as the no-backend fallback (plain text only - Android platform limit, told to him and acknowledged).
- Email route REVERSED: Patrick rejects third-party email services ("I don't trust them"). Live route is now Gmail API first-party from anotherpatkreation@gmail.com: Cloud project, gmail.send scope, consent screen PUBLISHED to production (Testing-mode refresh tokens expire every 7 days - verified in Google's docs - which would break floor week), personal-use exception means no verification, he clicks through the "unverified app" warning once. Worker mirrors the Graph OAuth flow at /auth/gmail/login+callback; refresh token stored in D1. Brevo code kept but dormant. Caveats noted: token dies if he changes the Gmail password; ~500 emails/day consumer Gmail cap.
- UI edits (his): "N names in the drawing" count removed from the Info screen and from under the SPIN button (he first asked, then said "nm I didn't see that" - with zero real entries those lines read differently, which explains the confusion; removed per his original ask; Players tab keeps a "N players" count). Winners tagged with trophy emoji instead of "WON". NEW GAME button in Settings now erases ALL players (confirm dialog) - fresh slate, per his correction. Players tab has EMAIL ME THE LIST: shares a real CSV (qr-wheel-players-<date>.csv, Excel-compatible, Name/Email/Won columns) via his mail app to anotherpatkreation@gmail.com until the backend can send it directly. androidx.core added for FileProvider (APK grew to ~1.8MB).
- Option button rows (his survey feature): up to 3 rows under name+email on the Info screen, each with its own on/off switch in Settings; 4 label fields per row - only labels WITH text become buttons (blank = hidden), buttons spread evenly at any count. Multi-select per row (his override of single-pick assumption); selected buttons show a green power sign (⏻, #6FBA2C) beside the label; tap again to deselect. Default: Row 1 on with IDG + ISG; rows 2-3 off. Answers save per player (multi-picks joined "; ") and export as Option 1/2/3 columns in the Excel CSV. Players tab unchanged per his instruction.

## v0.1.0-test9 (2026-11-05)
- Fourth survey row added (same rules as rows 1-3: per-row on/off, up to 4 buttons, blank label = hidden, multi-select, green dot indicator). Default off with blank labels.
- Info screen compacted for the razr: title 36sp->32sp, name/email fields 64dp->56dp, option rows 56dp->52dp with 8dp margins, ADD button 64dp->56dp. Net result: slimmer than test8 even with the extra row available.
- Export: Option 4 column in the CSV; opt4 persisted per player.

## v0.1.0-test10 (2026-11-05)
- Badge scanning: camera button above the Full name field opens the camera; on-device OCR (Google ML Kit, bundled Latin model - no network, no third-party service, no account) reads the badge.
- Autofill: name = the tallest non-email, non-phone, non-URL printed line (badges print the name biggest); email = printed email if found, otherwise generated as first-initial + last name @lenovo.com per Patrick's spec (2026-11-05 9:00 PM). Both fields stay editable before ADD.
- APK now arm64-v8a only (bundled OCR model); ~16MB vs ~1.8MB before. razr and all modern event phones are arm64.
