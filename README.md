# Literal Memo

Write. Throw. Search.

A minimalist text-based note app with Git sync.

<p>
  <a href="https://github.com/256x/memo/releases/latest"><img src="https://img.shields.io/github/v/release/256x/memo?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

<p>
  <img src="https://github.com/user-attachments/assets/9959d8e1-2007-4760-84cb-f1e91dac6c1d" width="180" alt="List screen" />
  <img src="https://github.com/user-attachments/assets/84ad1a98-b7c1-4e55-ab82-5b26ceeb9958" width="180" alt="Edit screen" />
  <img src="https://github.com/user-attachments/assets/58488527-2b91-433f-be86-cd4155f5b369" width="180" alt="Preview screen" />
  <img src="https://github.com/user-attachments/assets/cc3cc969-4ef2-407d-a25b-52a273b6667f" width="180" alt="Settings screen" />
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
- **Sync**: GitHub sync with auto-sync on launch and after editing
- **Customize**: Font, colors, left-hand mode
## How it works

Notes are stored in a simple directory structure:
```
repo/
├── pile/    ← active notes
└── trash/   ← deleted notes
```

- Both directories are synced.
- If a note exists in `trash/`, it is considered deleted everywhere.
- Deletion always wins.

No complex state management. No hidden metadata. Just files.

## Why no folders?

Because you don't use them.

You don't browse folders. You don't revisit archives. You search.

So that's what this app optimizes for.

## Why no restore?

Deleting a note moves it to `trash/`. That's it.

There is no restore button. No undo.

If you really need something back, find it in `trash/` in your repo and copy the content into a new note.

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

## Design Constraints

This app is intentionally simple.

If it needs heavy documentation or multiple language support, it's already too complicated.

## Usage

### Sharing Links

1. Find a link in your browser or any app
2. Tap Share → Literal Memo
3. A new memo is created with the link
4. View it later in Preview mode to open the link

### Multi-device Sync

GitHub Sync keeps your memos synchronized across devices:

- Syncs automatically on app launch and after editing
- Don't edit the same memo on multiple devices simultaneously
- If conflicts occur, the first device to sync wins
- All changes are preserved in Git history

For detailed sync behavior, see [User Guide](docs/USER_GUIDE.md).

## Credits

- **Inspired by [howm](https://kaoriya.github.io/howm/)** — a note-taking tool built on the idea of "write first, organize never."

## Development

Built with Kotlin and Jetpack Compose.

## Build

- Kotlin / Jetpack Compose
- Target: Android 8.0+

## License

MIT

## PC Scripts

A simple script to create a new memo from terminal:
```bash
#!/bin/bash
cd "$(dirname "$0")/../pile" || exit 1
nvim "$(date -u +%Y%m%d_%H%M%S).md"
```

See `scripts/new-memo.sh`. Tweak it however you like — you know what you're doing.
