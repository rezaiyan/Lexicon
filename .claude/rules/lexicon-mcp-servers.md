---
name: lexicon-mcp-servers
description: Project-specific MCP servers configured in .mcp.json (context7, github). Pilot-core MCP servers are documented in ~/.claude/rules/mcp-servers.md.
---

# Lexicon MCP Servers

**Last Updated:** 2026-04-08
**Config file:** `.mcp.json` (repo root)

Only user-added project MCP servers are documented here. Pilot-core MCP servers (mem-search, web-search, web-fetch, grep-mcp, codegraph) live in `~/.claude/rules/mcp-servers.md`.

---

## context7 — Library Documentation

**Status:** Working (smoke-tested 2026-04-08)
**Package:** `@upstash/context7-mcp` (via `npx -y`)

Fetches up-to-date docs and code examples for any library/framework/SDK. Prefer this over web search for library docs — even for well-known ones (React, Compose, Ktor, Koin) since training data may be stale.

**Use it for:** Kotlin Multiplatform docs, Compose Multiplatform APIs, Ktor client config, Koin DI patterns, SQLDelight schema syntax, androidx.navigation-compose, Firebase Auth, RevenueCat KMP, kotlinx.serialization.

**Don't use for:** refactoring, writing scripts from scratch, debugging business logic, or general programming concepts.

### Workflow (2 steps)

```
ToolSearch(query="+context7 resolve")

mcp__context7__resolve-library-id(libraryName="Compose Multiplatform")
# → returns libraryId (e.g., /jetbrains/compose-multiplatform)
mcp__context7__query-docs(libraryId="/jetbrains/compose-multiplatform", query="lazy list with sticky headers")
```

Smoke-test result: `resolve-library-id` query "Kotlin Multiplatform" returned 5 matches including JetBrains KMP docs, Android Developer KMP docs, and kotlinlang.org.

---

## github — GitHub MCP Server

**Status:** ✅ Working (fixed 2026-04-08)
**Binary:** `github-mcp-server` v0.33.0 (`brew install github-mcp-server`)

Official GitHub MCP server (`github/github-mcp-server`). Exposes tools for repos, issues, PRs, Actions, code search, and more.

**Token:** Sourced dynamically from `gh auth token` — no token stored in `.mcp.json`.

### Config (in `.mcp.json`)

```json
"github": {
  "command": "bash",
  "args": ["-c", "GITHUB_PERSONAL_ACCESS_TOKEN=$(gh auth token) /opt/homebrew/bin/github-mcp-server stdio"]
}
```

### Usage

Tools are auto-loaded when Claude Code starts. Use `ToolSearch` to discover them:

```
ToolSearch(query="+github list_issues")
ToolSearch(query="+github get_pull_request")
ToolSearch(query="+github search_code")
```

### Re-authentication

If `gh auth token` expires, run `gh auth login` to refresh. The MCP server picks up the new token on next session start.

---

## Adding a New MCP Server

1. Edit `.mcp.json` — add server under `mcpServers`
2. Restart Claude Code session (MCP servers load at startup)
3. Verify with `ToolSearch(query="+<server-name> <tool>")`
4. Document the server in this file with smoke-test results
