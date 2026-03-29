#!/bin/bash
# PostToolUse hook: run detekt after editing a Kotlin file.
# Tool input arrives as JSON on stdin.

input=$(cat)
file=$(echo "$input" | python3 -c "import sys,json; print(json.loads(sys.stdin.read()).get('file_path',''))" 2>/dev/null)

# Only trigger for .kt files
[[ "$file" != *.kt ]] && exit 0

cd "$(git rev-parse --show-toplevel)" || exit 0

output=$(./gradlew composeApp:detekt --quiet --continue 2>&1)
exit_code=$?

if [ $exit_code -ne 0 ]; then
  # Filter output to lines mentioning the changed file (basename match)
  basename=$(basename "$file")
  relevant=$(echo "$output" | grep -E "$basename|violation|error" | tail -30)
  echo "Detekt violations in $basename:" >&2
  echo "${relevant:-$output}" | tail -30 >&2
  exit 1
fi
