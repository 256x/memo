User Guide

Getting Started

1. Tap + to create a note
2. Write anything
3. Press back to save

That's it.

Search

Type in the search bar at the bottom. Press Enter.
Results show matching notes with highlighted snippets.
Press back to clear.

Deletion

Swipe left on any note to delete.
Deleted notes are moved to trash/ in your remote repository. They disappear from the app immediately.

There is no local trash. There is no restore button.

To recover a note, access your repo on PC and copy content from trash/ to a new file in pile/.

GitHub Sync

Setup

1. Create a GitHub repository (private recommended)
2. Generate a Personal Access Token:
   - Go to github.com > Settings > Developer settings > Personal access tokens > Tokens (classic)
   - Click "Generate new token (classic)"
   - Give it a name like "Literal Memo"
   - Select the "repo" scope (Full control of private repositories)
   - Click "Generate token"
   - Copy the token immediately (you won't see it again)
3. In the app, go to Settings > Connect GitHub
4. Paste your token and enter your repository name (e.g., username/notes)

Usage

Tap the sync icon to sync manually.
Edits: last write wins.
Deletions: delete wins.

Repository Structure

repo/
  pile/
    20260326_120000.md
    20260326_130000.md
  trash/
    20260326_110000.md

Multi-device

Sync before editing on a new device.
Don't edit the same note on multiple devices simultaneously.
Deletion propagates to all devices.

PC Usage

Edit notes directly:
  git pull
  (edit files in pile/)
  git add . && git commit -m "update" && git push

Permanently delete:
  rm trash/old_note.md
  git add . && git commit -m "cleanup" && git push

Recover a deleted note:
  cp trash/note.md pile/$(date +%Y%m%d_%H%M%S).md
  git add . && git commit -m "recover" && git push

Customization

Font: Default, Serif, or Monospace
Size: 12sp to 24sp
Colors: Background, Text, Accent

Tips

Write short notes. One idea per note.
Use descriptive first lines. They become titles.
Search is your friend. Don't organize.
Sync often on multiple devices.
