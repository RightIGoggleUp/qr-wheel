const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type,Authorization",
};

const DEFAULT_TPL_CONFIRM =
  "Hi {name}!\n\nThanks for entering - you're in today's prize drawing. Winners are announced by email, so keep an eye on your inbox.\n\nSee what we're about: {link}\n\n- Another Pat Kreation";
const DEFAULT_TPL_WINNER =
  "Hi {name}!\n\nCongratulations - you won today's prize drawing! Stop by our table to pick up your prize.\n\nThanks for entering!\n\n- Another Pat Kreation";
const DEFAULT_TPL_WINNER_SUBJECT = "You won the prize drawing!";
const DEFAULT_TPL_CONFIRM_SUBJECT = "You're entered to win!";

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", ...CORS },
  });
}

function authed(request, env) {
  const h = request.headers.get("Authorization") || "";
  const url = new URL(request.url);
  const token = h.startsWith("Bearer ") ? h.slice(7) : (url.searchParams.get("token") || "");
  return env.ADMIN_TOKEN && token === env.ADMIN_TOKEN;
}

async function getSetting(env, key, fallback = "") {
  const row = await env.DB.prepare("SELECT value FROM settings WHERE key = ?").bind(key).first();
  return row && row.value != null && row.value !== "" ? row.value : fallback;
}

async function putSetting(env, key, value) {
  await env.DB.prepare(
    "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value"
  ).bind(key, value).run();
}

function renderTemplate(tpl, vars) {
  return tpl.replace(/\{(\w+)\}/g, (m, k) => (k in vars ? vars[k] : m));
}

// ---- Microsoft Graph (send as the owner's work Outlook) ----

async function graphAccessToken(env) {
  const refresh = await getSetting(env, "ms_refresh_token");
  if (!refresh || !env.MS_CLIENT_ID || !env.MS_TENANT) return null;
  const res = await fetch(
    `https://login.microsoftonline.com/${env.MS_TENANT}/oauth2/v2.0/token`,
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: env.MS_CLIENT_ID,
        ...(env.MS_CLIENT_SECRET ? { client_secret: env.MS_CLIENT_SECRET } : {}),
        grant_type: "refresh_token",
        refresh_token: refresh,
        scope: "offline_access Mail.Send",
      }),
    }
  );
  if (!res.ok) return null;
  const data = await res.json();
  if (data.refresh_token) await putSetting(env, "ms_refresh_token", data.refresh_token);
  return data.access_token || null;
}

async function sendMsGraph(env, toEmail, toName, subject, text, html) {
  const token = await graphAccessToken(env);
  if (!token) return { sent: false, reason: "not_connected" };
  const res = await fetch("https://graph.microsoft.com/v1.0/me/sendMail", {
    method: "POST",
    headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        subject,
        body: { contentType: html ? "HTML" : "Text", content: html || text },
        toRecipients: [{ emailAddress: { address: toEmail, name: toName || "" } }],
      },
      saveToSentItems: true,
    }),
  });
  return { sent: res.status === 202, reason: res.status === 202 ? "ok" : "graph_" + res.status };
}

async function sendBrevo(env, toEmail, toName, subject, text, html) {
  if (!env.BREVO_API_KEY || !env.SENDER_EMAIL) return { sent: false, reason: "not_connected" };
  const payload = {
    sender: { email: env.SENDER_EMAIL, name: env.SENDER_NAME || "" },
    to: [{ email: toEmail, name: toName || "" }],
    subject,
    textContent: text,
  };
  if (html) payload.htmlContent = html;
  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: { "api-key": env.BREVO_API_KEY, "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return { sent: res.ok || res.status === 201, reason: res.ok || res.status === 201 ? "ok" : "brevo_" + res.status };
}

async function sendMail(env, toEmail, toName, subject, text, html) {
  const route = await getSetting(env, "sender_route", "ms");
  if (route === "brevo") return sendBrevo(env, toEmail, toName, subject, text, html);
  if (route === "none") return { sent: false, reason: "disabled" };
  return sendMsGraph(env, toEmail, toName, subject, text, html);
}

// ---- Router ----

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === "OPTIONS") return new Response(null, { headers: CORS });

    if (path === "/api/signup" && request.method === "POST") {
      let body;
      try { body = await request.json(); } catch { return json({ ok: false, error: "bad_json" }, 400); }
      const name = String(body.name || "").trim();
      const email = String(body.email || "").trim().toLowerCase();
      const sells = String(body.sells || "").trim();
      if (!name) return json({ ok: false, error: "name_required" }, 400);
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json({ ok: false, error: "email_invalid" }, 400);

      const existing = await env.DB.prepare("SELECT id FROM entries WHERE email = ?").bind(email).first();
      if (existing) return json({ ok: true, already: true });

      await env.DB.prepare("INSERT INTO entries (name, email, sells) VALUES (?, ?, ?)")
        .bind(name, email, sells).run();

      const link = await getSetting(env, "link");
      const tpl = await getSetting(env, "tpl_confirm", DEFAULT_TPL_CONFIRM);
      const subject = await getSetting(env, "tpl_confirm_subject", DEFAULT_TPL_CONFIRM_SUBJECT);
      const first = name.split(" ")[0] || name;
      let mail = { sent: false, reason: "not_connected" };
      try { mail = await sendMail(env, email, name, subject, renderTemplate(tpl, { name: first, link })); } catch (_) {}
      return json({ ok: true, emailed: mail.sent });
    }

    if (path === "/api/config" && request.method === "GET") {
      return json({
        link: await getSetting(env, "link"),
        sender_route: await getSetting(env, "sender_route", "ms"),
        ms_connected: !!(await getSetting(env, "ms_refresh_token")),
        brevo_connected: !!env.BREVO_API_KEY,
        mail_connected: (await getSetting(env, "sender_route", "ms")) === "brevo" ? !!env.BREVO_API_KEY : !!(await getSetting(env, "ms_refresh_token")),
      });
    }

    if (path === "/api/config" && request.method === "POST") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      let body;
      try { body = await request.json(); } catch { return json({ ok: false, error: "bad_json" }, 400); }
      if (typeof body.link === "string") await putSetting(env, "link", body.link.trim());
      if (typeof body.sender_route === "string" && ["ms", "brevo", "none"].includes(body.sender_route)) {
        await putSetting(env, "sender_route", body.sender_route);
      }
      return json({ ok: true });
    }

    if (path === "/api/templates" && request.method === "GET") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      return json({
        confirm: await getSetting(env, "tpl_confirm", DEFAULT_TPL_CONFIRM),
        confirm_subject: await getSetting(env, "tpl_confirm_subject", DEFAULT_TPL_CONFIRM_SUBJECT),
        winner: await getSetting(env, "tpl_winner", DEFAULT_TPL_WINNER),
        winner_subject: await getSetting(env, "tpl_winner_subject", DEFAULT_TPL_WINNER_SUBJECT),
        winner_html: await getSetting(env, "tpl_winner_html", ""),
      });
    }

    if (path === "/api/templates" && request.method === "POST") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      let body;
      try { body = await request.json(); } catch { return json({ ok: false, error: "bad_json" }, 400); }
      for (const [k, key] of [["confirm", "tpl_confirm"], ["confirm_subject", "tpl_confirm_subject"], ["winner", "tpl_winner"], ["winner_subject", "tpl_winner_subject"], ["winner_html", "tpl_winner_html"]]) {
        if (typeof body[k] === "string") await putSetting(env, key, body[k]);
      }
      return json({ ok: true });
    }

    if (path === "/api/entries" && request.method === "GET") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      const { results } = await env.DB.prepare(
        "SELECT id, name, email, sells, created_at FROM entries ORDER BY created_at ASC"
      ).all();
      return json(results);
    }

    if (path === "/api/entries.csv" && request.method === "GET") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      const { results } = await env.DB.prepare(
        "SELECT id, name, email, sells, created_at FROM entries ORDER BY created_at ASC"
      ).all();
      const esc = (v) => `"${String(v == null ? "" : v).replace(/"/g, '""')}"`;
      const rows = [["ID", "Name", "Work email", "What they sell", "Signed up (UTC)"].map(esc).join(",")];
      for (const r of results) rows.push([r.id, r.name, r.email, r.sells, r.created_at].map(esc).join(","));
      return new Response("\uFEFF" + rows.join("\r\n"), {
        headers: {
          "Content-Type": "text/csv; charset=utf-8",
          "Content-Disposition": "attachment; filename=signups.csv",
          ...CORS,
        },
      });
    }

    if (path === "/api/notify-winner" && request.method === "POST") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      let body;
      try { body = await request.json(); } catch { return json({ ok: false, error: "bad_json" }, 400); }
      const id = Number(body.entry_id);
      let entry = null;
      if (id) {
        entry = await env.DB.prepare("SELECT id, name, email FROM entries WHERE id = ?").bind(id).first();
      }
      const name = String((entry && entry.name) || body.name || "").trim();
      const email = String((entry && entry.email) || body.email || "").trim().toLowerCase();
      if (!email) return json({ ok: false, error: "email_required" }, 400);
      await env.DB.prepare("INSERT INTO winners (entry_id, name, email) VALUES (?, ?, ?)")
        .bind(entry ? entry.id : null, name, email).run();
      const link = await getSetting(env, "link");
      const tpl = await getSetting(env, "tpl_winner", DEFAULT_TPL_WINNER);
      const subject = await getSetting(env, "tpl_winner_subject", DEFAULT_TPL_WINNER_SUBJECT);
      const tplHtml = await getSetting(env, "tpl_winner_html", "");
      const first = name.split(" ")[0] || name;
      const origin = new URL(request.url).origin;
      const vars = { name: first, link, asset_base: origin };
      const mail = await sendMail(env, email, name, subject, renderTemplate(tpl, vars), tplHtml ? renderTemplate(tplHtml, vars) : null);
      return json({ ok: true, emailed: mail.sent, reason: mail.reason });
    }

    if (path === "/api/winners" && request.method === "GET") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      const { results } = await env.DB.prepare("SELECT * FROM winners ORDER BY drawn_at DESC").all();
      return json(results);
    }

    // ---- Outlook connect (OAuth) ----

    if (path === "/auth/login" && request.method === "GET") {
      if (!authed(request, env)) return json({ ok: false, error: "unauthorized" }, 401);
      if (!env.MS_CLIENT_ID || !env.MS_TENANT) return json({ ok: false, error: "ms_not_configured" }, 500);
      const redirect = url.origin + "/auth/callback";
      const authUrl =
        `https://login.microsoftonline.com/${env.MS_TENANT}/oauth2/v2.0/authorize` +
        `?client_id=${encodeURIComponent(env.MS_CLIENT_ID)}` +
        `&response_type=code&response_mode=query` +
        `&redirect_uri=${encodeURIComponent(redirect)}` +
        `&scope=${encodeURIComponent("offline_access Mail.Send")}`;
      return Response.redirect(authUrl, 302);
    }

    if (path === "/auth/callback" && request.method === "GET") {
      const code = url.searchParams.get("code");
      if (!code) return new Response("Missing code", { status: 400 });
      const redirect = url.origin + "/auth/callback";
      const res = await fetch(
        `https://login.microsoftonline.com/${env.MS_TENANT}/oauth2/v2.0/token`,
        {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({
            client_id: env.MS_CLIENT_ID,
            ...(env.MS_CLIENT_SECRET ? { client_secret: env.MS_CLIENT_SECRET } : {}),
            grant_type: "authorization_code",
            code,
            redirect_uri: redirect,
            scope: "offline_access Mail.Send",
          }),
        }
      );
      if (!res.ok) return new Response("Sign-in failed: " + (await res.text()), { status: 400 });
      const data = await res.json();
      if (data.refresh_token) await putSetting(env, "ms_refresh_token", data.refresh_token);
      return new Response(
        "<html><body style='background:#0A0A0F;color:#F5F5F7;font-family:sans-serif;display:flex;min-height:100vh;align-items:center;justify-content:center'><div style='text-align:center'><h1>Outlook connected</h1><p style='color:#9B9BA6'>Confirmation emails will now send from your work account. You can close this tab.</p></div></body></html>",
        { headers: { "Content-Type": "text/html" } }
      );
    }

    return json({ ok: false, error: "not_found" }, 404);
  },
};
