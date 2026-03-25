# Literal Memo

A simple, file-based Markdown memo app with GitHub sync.

<p>
  <a href="https://github.com/256x/memo/releases/latest"><img src="https://img.shields.io/github/v/release/256x/memo?label=GitHub%20Release"></a>&nbsp;<img src="https://img.shields.io/badge/Android-8%2B-blue">&nbsp;<img src="https://img.shields.io/badge/license-MIT-lightgrey">
</p>

[User Guide](./docs/USER_GUIDE.md)

## Features

- **File-Based**: Notes are stored as plain `.md` files, not in a database.
- **Markdown Preview**: Write in Markdown, preview with formatting.
- **GitHub Sync**: Sync your memos to any GitHub repository.
- **Customization**: Change background, text, and accent colors. Choose font and size.
- **Trash Management**: Deleted memos go to trash first. Restore or permanently delete anytime.

## Why file-based?

Inspired by [howm](https://howm.osdn.jp/), Literal Memo treats notes as simple text files.

- **Portable**: Your notes are just `.md` files. Move them anywhere.
- **No Lock-in**: No proprietary format. Edit with any text editor.
- **Git-Friendly**: Sync to GitHub and edit from your PC or any device.

## GitHub Sync

Memos sync to a `pile/` folder in your GitHub repository.

- Active memos: `pile/filename.md`
- Trashed memos: `pile/.filename.md` (dot prefix)

You can add or edit `.md` files directly on GitHub — they will sync to your device.

## Who is this for?

This app may appeal to people who:

- want a simple, distraction-free note-taking app
- prefer plain text files over database-locked notes
- like syncing notes via Git

## Credits

- **Inspired by [howm](https://howm.osdn.jp/)** — a note-taking tool based on the philosophy of "write anywhere, find anytime."

## Build

- Kotlin / Jetpack Compose
- Target: Android 8.0+
