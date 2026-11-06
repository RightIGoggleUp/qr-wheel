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
