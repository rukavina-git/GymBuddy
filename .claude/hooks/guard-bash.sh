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

if grep -Eq '(^|[^[:alnum:]_-])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push' <<<"$cmd"; then
  grep -Eq -- '(--force|--force-with-lease|--force-if-includes|[[:space:]]-[[:alnum:]]*f)' <<<"$cmd" \
    && block "force push"
  grep -Eq '[[:space:]]\+[^[:space:]]+' <<<"$cmd" && block "force push (+refspec)"
  grep -Eq -- '(--delete|[[:space:]]-d([[:space:]]|$)|[[:space:]]:[^[:space:]]+)' <<<"$cmd" \
    && block "remote branch deletion"
  grep -Eq -- '(^|[[:space:]:/])main([[:space:]]|$)' <<<"$cmd" && block "push to main"
  if [ "$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)" = "main" ]; then
    block "push while on main"
  fi
fi

grep -Eq -- '(^|[^[:alnum:]_-])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+branch[[:space:]].*(-d|-D|--delete)([[:space:]]|$)' <<<"$cmd" \
  && block "branch deletion"

# Secrets: the Read deny rules in settings.json don't cover shell access.
grep -Eq -- 'keystore\.properties|google-services\.json|service-account[^[:space:]]*\.json|firebase-adminsdk|\.jks([[:space:]"'\'']|$)|keystores/|(^|[[:space:]/"'\''])\.env([[:space:]"'\'']|$)|find-generic-password' <<<"$cmd" \
  && block "command touches a secret file or the keychain"

grep -Eiq -- '(^|[^[:alnum:]_-])(fastlane|deploy)([[:space:]]|$)|docker[[:space:]]+push|gh[[:space:]]+release|gradlew[^|;&]*[[:space:]]publish|--prod([[:space:]]|=|$)|[[:space:]]prod(uction)?([[:space:]]|$)' <<<"$cmd" \
  && block "deploy/prod command"

exit 0
