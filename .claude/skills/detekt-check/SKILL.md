---
name: detekt-check
description: Run detekt, report violations by file, and fix common issues (naming, formatting, unused imports)
argument-hint: "[file or module path to focus on]"
user-invocable: true
allowed-tools: ["Bash", "Read", "Edit", "Grep"]
---

Run Detekt on the Lexicon codebase and fix any violations.

## Steps

1. Run `./gradlew composeApp:detekt --continue 2>&1` and capture output
2. Parse violations — group them by file and rule name
3. Print a summary table: file | rule | line
4. For each violation, read the offending file and fix it:
   - **MagicNumber**: extract to a named constant
   - **MaxLineLength**: break the line at a natural boundary
   - **UnusedImports**: remove the import
   - **WildcardImport**: expand to explicit imports
   - **FunctionNaming / VariableNaming**: rename to match Kotlin conventions
   - **ComplexMethod / LongMethod**: note it but do NOT refactor without explicit user approval
5. Re-run `./gradlew composeApp:detekt --continue 2>&1` to confirm all fixed violations are gone
6. Report: "Fixed N violations across M files. K violations remain (require manual review)."

If $ARGUMENTS is provided, focus only on files matching that path pattern.