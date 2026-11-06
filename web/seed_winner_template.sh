#!/usr/bin/env bash
# Run ONCE after `wrangler deploy`, from the web/ directory.
# Seeds Patrick's formatted Outlook template (winner_template.html) into D1 as tpl_winner_html.
set -euo pipefail
cd "$(dirname "$0")"
python3 - <<'PY'
import json
html = open('winner_template.html').read()
sql = "INSERT INTO settings (key, value) VALUES ('tpl_winner_html', " + json.dumps(html) + ") " \
      "ON CONFLICT(key) DO UPDATE SET value=excluded.value;\n"
open('/tmp/seed_winner_template.sql','w').write(sql)
print("sql bytes:", len(sql))
PY
npx wrangler d1 execute qrwheel --remote --file /tmp/seed_winner_template.sql
echo "Seeded. Verify in the admin page: Winner email - rich HTML version should be filled."
