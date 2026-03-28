# User Guide

## Getting Started

1. Open the app
2. Tap + to create a note
3. Write anything
4. Press back to save

That's it.

## Search

Type in the search bar at the bottom. Press Enter.

Results show matching notes with highlighted snippets.

Press back to clear the search.

## Deletion

Swipe left on any note to delete.

Deleted notes move to `trash/`. They're gone from the app, but still exist in your repo.

There is no restore UI. If you need something back, find it in `trash/` in your repo and copy the content into a new note.

## GitHub Sync

### Setup

1. Go to Settings
2. Tap "Connect GitHub"
3. Enter your Personal Access Token (with `repo` scope)
4. Enter your repository name (e.g., `username/notes`)

### Usage

- Tap the sync icon to sync manually
- Sync pulls remote changes and pushes local changes
- Edits: last write wins
- Deletions: delete wins

### Repository Structure
```
repo/
├── pile/
│   ├── 20260326_120000.md
│   └── 20260326_130000.md
└── trash/
    └── 20260326_110000.md
```

### Multi-device Usage

- Sync before editing on a new device
- Don't edit the same note on multiple devices simultaneously
- Deletion propagates to all devices

### PC Usage

Edit notes directly:
```bash
git pull
# edit files in pile/
git add . && git commit -m "update" && git push
```

Permanently delete:
```bash
rm trash/old_note.md
git add . && git commit -m "cleanup" && git push
```

## Customization

- **Font**: Default, Serif, or Monospace
- **Size**: 12sp to 24sp
- **Colors**: Background, Text, Accent

## Tips

- Write short notes. One idea per note.
- Use descriptive first lines — they become titles.
- Search is your friend. Don't organize.
- Sync often on multiple devices.
