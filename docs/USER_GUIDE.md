# Literal Memo - User Guide

This guide explains how to use Literal Memo.

## 1. Creating a Memo

Tap the **+** button at the bottom right of the memo list.

- A new memo opens in **Edit mode** with the keyboard ready.
- Start writing. Your memo is saved automatically when you leave.

## 2. Memo List

The main screen shows all your memos, sorted by last modified date.

- **Tap**: Open the memo in Preview mode.
- **Swipe Left**: Move the memo to Trash.

## 3. Search

Type in the search bar at the bottom and press **Enter** to search.

- Search covers the entire content of your memos.
- Press the back button or clear the search to show all memos.

## 4. Editing

When viewing a memo:

- **Tap "Edit"**: Switch to Edit mode.
- **Tap "Preview"**: Switch back to Preview mode.
- **Markdown Toolbar**: Use the toolbar at the bottom to insert Markdown syntax.
  - Select text and tap a button to wrap it (e.g., bold, italic).
  - Tap without selection to insert at cursor position.

## 5. Deleting

- **From List**: Swipe left on a memo to move it to Trash.
- **From Edit Screen**: Tap the trash icon in the top right.

## 6. Trash

Access Trash from **Settings → View Trash**.

- **Restore**: Bring a memo back to the main list.
- **Delete**: Permanently remove a memo.
- **Empty Trash**: Delete all trashed memos at once.

## 7. GitHub Sync

Sync your memos to a GitHub repository for backup and cross-device access.

### Setup

1. Go to **Settings**.
2. Tap **Connect** under GitHub Sync.
3. Enter your **Personal Access Token** (with `repo` scope) and **Repository** (format: `owner/repo`).

### How It Works

- **Sync Now**: Manually sync your memos.
- Active memos are stored in `pile/filename.md`.
- Trashed memos are stored as `pile/.filename.md` (hidden files).
- You can add or edit `.md` files directly on GitHub — they will download on the next sync.

### Permanent Delete

When you permanently delete a memo, it is removed from both your device and GitHub.

## 8. Settings

### Appearance

- **Font**: Choose Default, Serif, or Monospace.
- **Size**: Adjust text size with the slider.
- **Colors**: Tap BG, Text, or Accent to customize colors.

### GitHub Sync

- **Sync Now**: Trigger a manual sync.
- **Edit**: Change your token or repository.
- **Disconnect**: Remove GitHub connection.

---

*Memos are saved with timestamp filenames (e.g., `20260324_143000.md`).*
