# blame-aware-chat

![Build](https://github.com/mark00vka/blame-aware-chat/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
An IntelliJ plugin that lets you ask an LLM about any Kotlin function — with git blame context automatically included.

Place your caret inside a function, type a question, and the plugin builds a rich prompt containing the function body annotated with commit history, sibling signatures, called function signatures, and imports. The LLM sees *who changed what and why*, not just the raw code.
<!-- Plugin description end -->

## Why

LLM context windows are finite, and token budgets are shrinking. If you've hit usage limits trying to feed an entire codebase to an agent, you know the pain. This plugin takes the opposite approach: instead of dumping everything in and hoping the model figures it out, it surgically extracts only the function you care about, its call graph, and the git history behind each line — then sends that compact, high-signal prompt to the LLM. Fewer tokens, more relevance.

## How it works

1. **Context packing** — The plugin inspects the Kotlin PSI tree at the caret position and collects:
   - The full target function body
   - Import statements from the file
   - Signatures of sibling functions (bodies omitted)
   - Signatures of same-module functions called by the target (up to 15)

2. **Blame annotation** — Each line of the target function is annotated with its git blame info (short commit hash + commit subject). Consecutive lines from the same commit are grouped under a single header, keeping the prompt compact.

3. **LLM query** — The assembled prompt is sent to the configured provider and the response is displayed in the tool window.

## Supported LLM providers

| Provider | Model | Key required |
|----------|-------|:------------:|
| Claude (Anthropic) | `claude-sonnet-4-6` | Yes |
| Gemini (Google AI Studio) | `gemini-2.5-flash` | Yes |
| Ollama (local) | `qwen2.5-coder:7b` | No |

## Getting started

1. Install the plugin (see [Installation](#installation) below).
2. Open **Settings** > **Tools** > **Blame-Aware Chat**.
3. Pick a provider and enter an API key (or just use Ollama locally).
4. Open a Kotlin file and place your caret inside a function.
5. Open the **Blame-Aware Chat** tool window (right sidebar).
6. Type a question and click **Ask** — or click **Preview context** to inspect the prompt before sending.

## Installation

- **From the IDE:**
  <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > search for `blame-aware-chat` > <kbd>Install</kbd>

- **From JetBrains Marketplace:**
  Visit the [plugin page](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and click <kbd>Install to ...</kbd>.

- **From disk:**
  Download the [latest release](https://github.com/mark00vka/blame-aware-chat/releases/latest) and install via
  <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Gear icon</kbd> > <kbd>Install plugin from disk...</kbd>

## Requirements

- IntelliJ IDEA 2025.2+
- Kotlin plugin enabled
- Git4Idea plugin enabled (bundled with IDEA)

## Building from source

```bash
./gradlew buildPlugin
```

The distributable zip will be in `build/distributions/`.

---

Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
