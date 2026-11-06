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
