# Literal Memo

Write. Throw. Search.

A minimalist text-based note app with Git sync.

<p>
  <a href="https://github.com/256x/memo/releases/latest"><img src="https://img.shields.io/github/v/release/256x/memo?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

<p>
<img width="180" alt="List screen" src="https://github.com/user-attachments/assets/fe8a258c-2626-4b06-9321-8abf9a828a0e" />
<img width="180" alt="Preview screen" src="https://github.com/user-attachments/assets/16d7dde8-5da1-4fac-9e64-542087cc41e2" />
<img width="180" alt="Edit screen" src="https://github.com/user-attachments/assets/7cc5ac37-548f-4e9d-809d-e2d64b4a83ff" />
<img width="180" alt="setting screen" src="https://github.com/user-attachments/assets/b6dd2944-d276-4d3a-9377-aac28ea56b48" />
</p>

[User Guide](./docs/USER_GUIDE.md)

## Why?

Have you ever opened your archived notes in Keep?

Do you hesitate to write private thoughts into cloud services?

But you still want your notes available everywhere.

Your photos are already synced. Why not your notes — on your own terms?

What if notes didn't need organizing at all?

What if you could just write, and search when needed?

## The Idea

Literal Memo is built on a simple principle:

**Write anything. Throw it in. Search when needed. Delete when you don't.**

No folders. No tags. No archive. No restore.

Just text.
## Features

- **Write**: Minimal markdown editor with toolbar
- **Search**: Full-text search across all memos
- **Share**: Receive URLs and text from other apps
- **Sync**: Git sync (GitHub, Gitea/Forgejo, Codeberg) with auto-sync on launch and after editing
- **Customize**: Font, colors, left-hand mode
## How it works

Notes are stored in a simple directory structure:
```
repo/
├── pile/    ← active notes
└── trash/   ← deleted notes (Git sync only, for recovery)
```

No complex state management. No hidden metadata. Just files.

## Why no folders?

Because you don't use them.

You don't browse folders. You don't revisit archives. You search.

So that's what this app optimizes for.

## Why no restore?

There is no restore button. No undo.

Deletion removes the note from the app immediately. If Git Sync is enabled, the file is moved to `trash/` in your repository — not permanently deleted, just out of the way.

If you really need something back, move it from `trash/` back to `pile/` in your repo and sync.

This is intentional. Deletion should feel final. It keeps your pile clean.

## Who is this for?

This app may appeal to people who:

- prefer plain text over structured systems
- want full control over their data
- use search instead of navigation
- like simple, predictable tools
- are comfortable with Git-based workflows

## Philosophy

Literal Memo is not about organizing notes.

It's about not needing to.

This app is intentionally simple. If a feature requires explaining in multiple steps, it probably doesn't belong here.

## Usage

### Sharing Links

1. Find a link in your browser or any app
2. Tap Share → Literal Memo
3. A new memo is created with the link
4. View it later in Preview mode to open the link

### Multi-device Sync

Git Sync keeps your memos synchronized across devices:

- Syncs automatically on app launch and after editing
- Don't edit the same memo on multiple devices simultaneously
- If conflicts occur, the first device to sync wins
- All changes are preserved in Git history

For detailed sync behavior, see [User Guide](docs/USER_GUIDE.md).

## Credits

- **Inspired by [howm](https://kaoriya.github.io/howm/)** — a note-taking tool built on the idea of "write first, organize never."

## Development

- Kotlin / Jetpack Compose
- Target: Android 8.0+
- No Google APIs. No Firebase. No tracking.

This app was built with substantial assistance from [Claude](https://claude.ai) (Anthropic). AI was involved throughout development, including writing code.

## License

MIT

## PC Scripts

The `scripts/` directory has a few extras for terminal and editor use:

- `new-memo.sh` — create a new memo from terminal
- `search-memo.sh` — search memos with fzf
- `sync-memo.sh` — sync pile with remote repository
- `literalmemo.vim` — Vim integration (new / list / search)
- `literalmemo.lua` — Neovim integration via fzf-lua

See [User Guide](./docs/USER_GUIDE.md) for details.
