# Literal Memo

Write. Throw. Search.

A minimalist text-based note app with Git sync.

<p>
  <a href="https://github.com/256x/memo/releases/latest"><img src="https://img.shields.io/github/v/release/256x/memo?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

<p>
  <img src="https://github.com/user-attachments/assets/15ec5bf8-812e-460a-8ee8-fe8189b70650" width="180" />
  <img src="https://github.com/user-attachments/assets/4081ba6f-22f6-44db-85aa-f6357fbed887" width="180" />
  <img src="https://github.com/user-attachments/assets/539c1894-619f-4801-9947-97e834d2cdee" width="180" />
  <img src="https://github.com/user-attachments/assets/3e789a4c-66c9-4da5-9562-2f71764aa9ed" width="180" />
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

- **Plain Text**: Every note is a simple `.md` file.
- **Git Sync**: Sync notes across devices via your own GitHub repo.
- **Search**: Find anything instantly with full-text search.
- **Customization**: Change font, colors, and text size.

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

If you really need something back, open the repo on your PC, find it in `trash/`, and copy the content into a new note.

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
