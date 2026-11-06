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
- Badge scanning: camera button above the Full name field opens the camera; on-device OCR (Google ML Kit - on-device recognition, no third-party service, no account) reads the badge.
- Autofill: name = the tallest non-email, non-phone, non-URL printed line (badges print the name biggest); email = printed email if found, otherwise generated as first-initial + last name @lenovo.com per Patrick's spec (2026-11-05 9:00 PM). Both fields stay editable before ADD.
- APK now arm64-v8a only (bundled OCR model); ~16MB vs ~1.8MB before. razr and all modern event phones are arm64.

### test10 amendment
- Switched OCR to the thin Play Services ML Kit variant (same on-device API; model downloads on first scan, cached after). APK 16MB -> 3.5MB. Bundled 16MB builds failed GitHub release upload repeatedly (silent + spurious "big file" errors).

## Product name: Floor Day (2026-11-05)
- Patrick named the app "Floor Day" (WhatsApp 9:03:51 PM). Designated home: anotherpatkreation.com/floorday (domain mid-transfer; no DNS work yet).
- Repo stays RightIGoggleUp/qr-wheel for now; product name vs repo name noted.

## v0.1.0-test11 (2026-11-05)
- Rebrand: display name "Floor Day" (launcher + system), Excel export email subject "Floor Day players - <date>". Package id unchanged (com.anotherpatkreation.qrwheel).

## v0.1.0-test12 (2026-11-05)
- Privacy and Legal page: link at the bottom of Settings opens an in-app page (C90 pattern: in-app, no GitHub links, anotherpatkreation.com/floorday as the only pointer, contact contactme@anotherpatkreation.com).
- Store-ready content: on-device storage, badge scan facts (no camera permission held, on-device ML Kit, one-time model download, scan photo deleted right after reading), no accounts/analytics/ads/tracking, data leaves only via user-sent emails, internet-only permission, deletion via NEW GAME/uninstall, children/changes/contact/legal sections.
- MainActivity: each scan photo is deleted after OCR completes (was kept in cache before).

## v0.1.0-test13 (2026-11-05)
- Email generation rule fixed: only from a real first+last pair (2+ words); a single scanned name leaves the email blank (was ppatrick@lenovo.com).
- Scan success/interim toasts removed (Patrick: "I don't truly want the messages at the bottom"); no keyboard popup after a scan. Failure toasts kept (actionable).
- Name picking: blocklist for badge noise (Lenovo/ThinkPad/roles like EXHIBITOR/STAFF), and among the biggest lines it now prefers a two-word "First Last" line.

### test13 additions (same build)
- Settings now has LAST SCAN - WHAT IT READ: raw OCR text from the last badge scan, for debugging name picks (Patrick asked for visibility, 9:23 PM).
- Name picking merges stacked same-size lines ("Patrick" over "Kea" -> "Patrick Kea"; his real badge prints stacked). Orientation: ML Kit handles rotated photos natively.

## v0.1.0-test14 (2026-11-05)
- Brand spelling confirmed by Patrick: "FloorDay" ONE WORD (WhatsApp 9:32:07 PM). Info screen header now: FloorDay (40sp bold) / "by AnotherPatKreation" (14sp dim) / form subtitle. Launcher name, export subject, and header all use FloorDay.

## v0.1.0-test15 (2026-11-05)
- Launcher icon: "FloorDay" wordmark (Floor white / Day red on midnight), replacing the red-wheel glyph (Patrick: "make it say FloorDay").

## v0.1.0-test16 (Nov 5, 2026) - Local-first email, backend removed
- DECISION: Backend email architecture removed entirely per Patrick's Nov 5 10:20 PM spec. No Cloudflare worker, no Graph, no Gmail API, no OAuth, no admin tokens. App is fully local-first.
- Deleted: Api.kt and the whole web/ directory (worker, pages, schema, scripts).
- NEW: PREPARE BCC EMAIL button on the Players tab. Opens the organizer's own mail app via ACTION_SEND (text/html, createChooser) with the organizer in To and every valid, deduped participant email in BCC. Never sends automatically - Patrick reviews and taps send in his mail app.
- NEW: Settings > PARTICIPANT EMAIL section: organizer email (To), participant subject, participant HTML template, all stored locally on device. Defaults: "Thank you for joining us at FloorDay" + lemonade-quote signature block.
- CHANGED: Winner notify on the Wheel now opens the winner email (winner subject/body templates) in the mail app too - no backend call. Mark-won and dialog behavior unchanged.
- CHANGED: Settings no longer has API base URL or admin token fields. QR link pencil editor unchanged.
- FIX: Settings option-rows text now reads "Up to 4 rows, 1-4 buttons each." (was "Up to 3 rows").
- Export button restyled dark + renamed EXPORT PLAYER LIST (was red EMAIL ME THE LIST).

## v0.1.0-test17 (Nov 5, 2026) - BCC intent aligned to Patrick's reference code
- CHANGED: prepareBccEmail() now matches Patrick's reference implementation verbatim: EXTRA_TEXT carries the HtmlCompat.fromHtml formatted text (not a trimmed plain string), EXTRA_HTML_TEXT carries the raw HTML, chooser title is "Send participant email with".
- Live test feedback (Patrick, Nov 5 ~10:36 PM): BCC handoff passed in Outlook for Android (To/BCC/subject/chooser/manual send all correct - locked behavior), but the HTML body rendered blank. Fix per his spec: EXTRA_TEXT now carries the HtmlCompat.fromHtml Spanned (formatted fallback), EXTRA_HTML_TEXT carries the original HTML string unmodified. Default participant template replaced with his FloorDay Test template (bold/italic/link checks) to isolate whether Outlook or FloorDay is the limit.
- BUILD: test17 ships as a DEBUG APK per his build requirement (same debug signing key as all prior builds - installs in place, data preserved).

## v0.1.0-test18 (Nov 6, 2026) - Quadrant QR page (approved mockup)
- DECISION: QR page rebuilt to Patrick's approved mockup (his "Yes", Nov 6 9:56 AM). Foldables/tablets are the primary target per his clarification; phones are the smaller-screen fallback. Fixed 2x2 quadrant grid on both, cards scale with screen size.
- NEW: Up to 4 independent QR codes in quadrants, each with its own link + label below the code. Empty-link slots hide themselves. Slot 1 is the existing signup link (top-left), so nothing he configured breaks.
- NEW: Editable page title at top (Settings > QR PAGE, default "FloorDay").
- NEW: Custom full-page background image (Settings > QR PAGE > CHOOSE BACKGROUND IMAGE, copied into app storage, dark scrim keeps codes scannable). Removable.
- CHANGED: Tap any code (or the pencil) to edit its link + label in place - the two-tap pattern he validated.
- REMOVED: old single Lenovo-art QrCardView (plus lenovo_logo asset and dialog_link.xml), genuinely unused after the redesign, per his cleanup rule.
- Local-first, no backend/auth changes; BCC/email behavior untouched.

## v0.1.0-test19 (Nov 6, 2026) - QR label clipping fix
- FIX: Patrick's device screenshot (razr, ~3x density) showed the label under a code vertically clipped. Root cause: card-side math ignored density - the 46dp label block + card overflowed each grid row, and the cell clipped the label. Card side now subtracts the label block and system-bar/tab reserves in dp-correct pixels.
- Custom background image confirmed working on his device.
- NEW (same release, his 10:27 AM spec): pencil now opens a QR CODE 1/2/3/4 picker (all four slots, configured or empty), then link + name editing - same pattern as the Info-page option buttons. Labels capped by real font metrics to what fits two rows under the card; render clamps to 2 lines with ellipsis as a second guard.
- PARKED (roadmap, his 10:28 AM idea): multi-device data combine/sync for ~10 collectors at one event. Not implemented; no cloud permission. Merge-after-collection vs live sync to be decided later with privacy rules intact.

## v0.1.0-test20 (Nov 6, 2026) - precise two-line label fit
- FIX: test19's average-char label cap could still overflow two rows for wide glyphs (silent ellipsis). Replaced with an InputFilter that measures each candidate string through StaticLayout - the same line-breaking engine the label TextView uses - at the label's real cell width and with its real paint (bold 16sp, system font scale included). Input needing a third line is rejected as typed. Existing saved labels are never altered by the filter; a legacy overlong label shows an ellipsis until edited.

## v0.1.0-test21 (2026-11-06)
- Patrick 10:36:44 AM: all four QR boxes + codes as large as possible, ALL corners rounded (screenshot showed square top corners from overflow clipping), and each link generating a custom QR "as it was in the original".
- Restored the original custom QR art (QrCardView + Lenovo red band + round modules + red finder rings, EC level H), recovered from git history (pre-707e6d5). Each slot encodes its own saved link (Prefs.qrSlotLink(i)) - per-slot encoding verified in code.
- Sizing math: maxCardH = rowH - labelBlock(54dp) - 8dp, maxCardW = cellW - 12dp, no min-floor (old floor caused overflow + top-corner clipping). Cards now maximize within the real content area with full two-line labels and quiet zones.
- Patrick 10:39:00 AM: hard maximum CHARACTER LENGTH for labels, no shortening/ellipsis. maxChars = 2 x floor(cellW / width("W")) at the real bold 16sp paint (system font scale included) - worst-case-glyph guarantee that any in-cap string fits two lines. Live "n / max characters" counter under the editor field, red at the cap. StaticLayout check retained as second guard for glyphs wider than W. Legacy saved labels never rewritten; over-cap legacy label shows red counter, insertions reject until within cap.
- Removed: bg_qr_card.xml, qrBitmap(), lastQrSidePx, stale FrameLayout import.
