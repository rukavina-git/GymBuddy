#!/usr/bin/env python3
"""Builds the public legal pages from legal/*.md (Plan 9 step 02).

    python3 -I legal/build.py            # writes legal/site/gym-tracker/
    python3 -I legal/build.py --check    # only validates, writes nothing

No dependencies: a small Markdown subset (headings, paragraphs, "- "
lists, pipe tables, **bold**, [links](url)). Each page must start with
"Last updated: YYYY-MM-DD"; the build fails otherwise. Output paths
are extensionless directories (privacy/index.html) so the URLs are
https://rukavina.app/gym-tracker/privacy and /terms. The app name
lives in APP_NAME only: the texts say {app} (and {base} for the URL
path), so a rename is one line here plus brand_name in the app.
"""
import html
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent
APP_NAME = "Gym Tracker"
SLUG = "gym-tracker"
BASE = f"/{SLUG}"
PAGES = ["privacy", "terms"]
OUT = ROOT / "site" / SLUG
DATE_LINE = re.compile(r"^Last updated: (\d{4}-\d{2}-\d{2})$")

CSS = """
:root { color-scheme: dark; }
body { margin: 0; background: #121418; color: #C9CDD4;
  font: 16px/1.6 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
main { max-width: 680px; margin: 0 auto; padding: 32px 16px 64px; }
h1, h2 { color: #F3F4F6; line-height: 1.25; }
h1 { font-size: 28px; margin: 8px 0 24px; }
h2 { font-size: 20px; margin: 32px 0 8px; }
strong { color: #F3F4F6; }
a { color: #4D86F0; }
.date { color: #9AA1AD; font-size: 14px; }
table { border-collapse: collapse; width: 100%; font-size: 15px; }
th, td { text-align: left; padding: 8px 8px 8px 0;
  border-bottom: 1px solid #2A2E37; vertical-align: top; }
th { color: #F3F4F6; }
"""


def inline(text: str) -> str:
    out = html.escape(text, quote=False)
    out = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", out)
    return re.sub(r"\[(.+?)\]\((.+?)\)",
                  lambda m: f'<a href="{html.escape(m[2])}">{m[1]}</a>', out)


def render(lines: list[str]) -> str:
    out, para, items, rows = [], [], [], []

    def flush():
        if para:
            out.append(f"<p>{inline(' '.join(para))}</p>")
            para.clear()
        if items:
            out.append("<ul>" + "".join(f"<li>{inline(i)}</li>" for i in items) + "</ul>")
            items.clear()
        if rows:
            head, body = rows[0], rows[2:]
            out.append("<table><thead><tr>" + "".join(f"<th>{inline(c)}</th>" for c in head)
                       + "</tr></thead><tbody>"
                       + "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>"
                                 for r in body)
                       + "</tbody></table>")
            rows.clear()

    for raw in lines:
        line = raw.rstrip()
        if not line:
            flush()
        elif line.startswith("## "):
            flush(); out.append(f"<h2>{inline(line[3:])}</h2>")
        elif line.startswith("# "):
            flush(); out.append(f"<h1>{inline(line[2:])}</h1>")
        elif line.startswith("|"):
            if para or items:
                flush()
            rows.append([c.strip() for c in line.strip("|").split("|")])
        elif line.startswith("- "):
            if para:
                flush()
            items.append(line[2:])
        elif line.startswith("  ") and items:
            items[-1] += " " + line.strip()
        else:
            para.append(line)
    flush()
    return "\n".join(out)


def build(name: str) -> str:
    text = (ROOT / f"{name}.md").read_text(encoding="utf-8")
    if "GymBuddy" in text or "Gym Buddy" in text:
        sys.exit(f"error: legal/{name}.md names the old app; write {{app}}")
    src = text.replace("{app}", APP_NAME).replace("{base}", BASE).splitlines()
    m = DATE_LINE.match(src[0]) if src else None
    if not m:
        sys.exit(f"error: legal/{name}.md must start with 'Last updated: YYYY-MM-DD'")
    title_line = next((l for l in src if l.startswith("# ")), None)
    if not title_line:
        sys.exit(f"error: legal/{name}.md has no '# ' title")
    title = html.escape(title_line[2:])
    body = render(src[1:])
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<link rel="stylesheet" href="{BASE}/legal.css">
</head>
<body>
<main>
<p class="date">Last updated: {m[1]}</p>
{body}
</main>
</body>
</html>
"""


def main() -> None:
    check = "--check" in sys.argv[1:]
    pages = {name: build(name) for name in PAGES}
    if check:
        print("legal pages ok: " + ", ".join(PAGES))
        return
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "legal.css").write_text(CSS.lstrip(), encoding="utf-8")
    for name, page in pages.items():
        (OUT / name).mkdir(exist_ok=True)
        (OUT / name / "index.html").write_text(page, encoding="utf-8")
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
