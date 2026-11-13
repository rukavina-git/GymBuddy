#!/bin/bash
# PreToolUse guard for Bash. Blocks commands that are hard to undo or reach
# outside this machine. Exit 2 = blocked; stderr goes back to the agent.

cmd=$(jq -r '.tool_input.command // empty')
[ -z "$cmd" ] && exit 0

block() {
  echo "Blocked by .claude/hooks/guard-bash.sh: $1." >&2
  echo "Do not retry or work around this. Ask the user to run it themselves." >&2
  exit 2
}

repo="${CLAUDE_PROJECT_DIR:-.}"
ask_reason=""

# A feature/* branch already merged into develop may be deleted, but only
# after the user approves the prompt (never auto-allowed).
merged_feature() {
  local name=$1 ref=$2 base=$3
  [[ $name == feature/* ]] || return 1
  git -C "$repo" rev-parse -q --verify "$ref" >/dev/null || return 1
  git -C "$repo" merge-base --is-ancestor "$ref" "$base" 2>/dev/null
}

# Check only the git push segment(s), so flags elsewhere in a chained
# command (e.g. `rm -f x && git push`) don't trip the push rules.
push_re='(^|[^[:alnum:]_-])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push'
while IFS= read -r seg; do
  grep -Eq "$push_re" <<<"$seg" || continue
  push=$(sed -E 's/.*git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push/push/' <<<"$seg")
  grep -Eq -- '(--force|--force-with-lease|--force-if-includes|[[:space:]]-[[:alnum:]]*f)' <<<"$push" \
    && block "force push"
  grep -Eq '[[:space:]]\+[^[:space:]]+' <<<"$push" && block "force push (+refspec)"
  if grep -Eq -- '(--delete|[[:space:]]-d([[:space:]]|$)|[[:space:]]:[^[:space:]]+)' <<<"$push"; then
    # Only shape allowed: git push origin --delete feature/x [feature/y ...]
    grep -Eq -- '^push[[:space:]]+origin[[:space:]]+(--delete|-d)([[:space:]]+[^[:space:]-][^[:space:]]*)+[[:space:]]*$' <<<"$push" \
      || block "remote branch deletion"
    git -C "$repo" fetch -q origin develop 2>/dev/null
    for b in $(sed -E 's/^push[[:space:]]+origin[[:space:]]+(--delete|-d)//' <<<"$push"); do
      merged_feature "$b" "refs/remotes/origin/$b" origin/develop \
        || block "remote branch deletion ($b is not a feature/* branch merged into develop)"
    done
    ask_reason="Delete merged feature branch(es)? Approve only if you asked for it."
  fi
  grep -Eq -- '(^|[[:space:]:/])main([[:space:]]|$)' <<<"$push" && block "push to main"
  if [ "$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)" = "main" ]; then
    block "push while on main"
  fi
done < <(sed -E 's/(&&|\|\||;|\|)/\n/g' <<<"$cmd")

while IFS= read -r seg; do
  grep -Eq -- '(^|[^[:alnum:]_-])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+branch[[:space:]].*(-d|-D|--delete)([[:space:]]|$)' <<<"$seg" || continue
  # Only shape allowed: git branch -d feature/x [...] (never -D / force).
  grep -Eq -- '^[[:space:]]*git[[:space:]]+branch[[:space:]]+(-d|--delete)([[:space:]]+[^[:space:]-][^[:space:]]*)+[[:space:]]*$' <<<"$seg" \
    || block "branch deletion"
  for b in $(sed -E 's/^[[:space:]]*git[[:space:]]+branch[[:space:]]+(-d|--delete)//' <<<"$seg"); do
    merged_feature "$b" "refs/heads/$b" develop \
      || block "branch deletion ($b is not a feature/* branch merged into develop)"
  done
  ask_reason="Delete merged feature branch(es)? Approve only if you asked for it."
done < <(sed -E 's/(&&|\|\||;|\|)/\n/g' <<<"$cmd")

# Secrets: the Read deny rules in settings.json don't cover shell access.
grep -Eq -- 'keystore\.properties|google-services\.json|service-account[^[:space:]]*\.json|firebase-adminsdk|\.jks([[:space:]"'\'']|$)|keystores/|(^|[[:space:]/"'\''])\.env([[:space:]"'\'']|$)|find-generic-password' <<<"$cmd" \
  && block "command touches a secret file or the keychain"

# Deploy/prod check ignores prose: heredoc bodies (file content) and the
# quoted text after -m/--message/--body/--title/--notes/echo/printf
# (commit messages, PR text). Other quoted strings, e.g. bash -c "...",
# are still checked.
scan=$(perl -0777 -pe '
  s/(<<-?[ \t]*([\x27"]?)(\w+)\2)([^\n]*)\n.*?\n[ \t]*\3(?=\n|$)/$1$4/gs;
  s/(-m|--message|--body|--title|--notes|echo|printf)([ \t]+|=)("(?:\\.|[^"\\])*"|\x27[^\x27]*\x27)/$1 MSG/gs;
' <<<"$cmd")
# fastlane: only read-only subcommands pass (lanes, list, env, help,
# version). Lanes upload to testers/Play, so `fastlane test`, a bare
# `fastlane` and anything else stay blocked. Only fastlane in command
# position counts (segment start, after a quote, `bundle exec`, `exec`,
# `env`/VAR=x prefixes), so paths, `which fastlane` and grep patterns pass.
perl -e '
  $_ = join "", <STDIN>;
  my $pre = qr/(?:^|[;&|(\x27"`\n])[ \t]*(?:(?:env|exec|time|bundle[ \t]+exec|\w+=\S*)[ \t]+)*/m;
  while (/${pre}fastlane(?:[ \t]+(\S+))?(?=[\s;&|\x27")`]|$)/gi) {
    my $sub = defined $1 ? $1 : "";
    $sub =~ s/["\x27)]+$//;
    exit 1 unless $sub =~ /^(?:lanes|list|env|help|--help|-h|--version|-v|action|actions|search_plugins)$/;
  }
  exit 0;
' <<<"$scan" || block "fastlane lane (uploads); read-only subcommands are allowed"
grep -Eiq -- '(^|[^[:alnum:]_-])deploy([[:space:]]|$)|docker[[:space:]]+push|gh[[:space:]]+release|gradlew[^|;&]*[[:space:]]publish|--prod([[:space:]]|=|$)|[[:space:]]prod(uction)?([[:space:]]|$)' <<<"$scan" \
  && block "deploy/prod command"

# Merged feature-branch deletion: always the user's call via the prompt.
if [ -n "$ask_reason" ]; then
  jq -n --arg r "$ask_reason" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $r}}'
fi
exit 0
