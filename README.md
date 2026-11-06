# QR Wheel

QR-code signup + prize wheel for event booths, by Another Pat Kreation.

## Pieces

- `app/` - Android app (Kotlin, minSdk 26). Two tabs: a full-screen QR code that
  points at the signup page, and a spinning prize wheel that draws from the
  stored signups. Spin picks a winner; a pop-up asks whether to notify them,
  and the backend emails them. Settings (gear icon) hold the signup URL, the
  backend API base URL, and the admin token. With no backend set, the wheel
  runs on demo names.
- `web/` - Cloudflare Worker + static assets. The signup page (`/`), the admin
  page (`/admin`), and the JSON API (`/api/*`). Signups live in a D1 (SQLite)
  database. CSV export at `/api/entries.csv` opens in Excel.

## Signup flow

1. Attendee scans the QR code on the presenter's phone.
2. They fill in name, work email, and what they sell.
3. On submit the backend stores the entry and emails them a confirmation
   (templated, sent as the presenter's work Outlook via Microsoft Graph).
4. The success screen shows the destination link set in the admin page.

## Backend setup (Cloudflare free tier)

1. Create a D1 database named `qrwheel` and put its ID in `wrangler.toml`.
2. `npx wrangler d1 execute qrwheel --remote --file=web/schema.sql`
3. `npx wrangler deploy` from `web/`.
4. Secrets: `npx wrangler secret put ADMIN_TOKEN` and
   `npx wrangler secret put MS_CLIENT_SECRET`.
5. Vars in `wrangler.toml`: `MS_CLIENT_ID`, `MS_TENANT` (for Outlook sending).
6. In `/admin`: set the destination link, edit email templates, connect Outlook.

## Outlook (Microsoft Graph) setup

Emails send as the presenter's work account with delegated `Mail.Send`:

1. In Microsoft Entra (work tenant), register an app ("QR Wheel Mailer") with a
   Web redirect URI of `https://<deployed-site>/auth/callback` and a client secret.
2. Add delegated permission: Microsoft Graph `Mail.Send`; grant consent
   (may need IT admin approval depending on tenant policy).
3. Set `MS_CLIENT_ID`, `MS_TENANT`, and `MS_CLIENT_SECRET` on the Worker.
4. Open `/admin` and click "Connect work Outlook", then sign in once.

## Build the app

Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0):

    cd app && ../gradlew assembleRelease

The APK lands in `app/build/outputs/apk/release/`.
