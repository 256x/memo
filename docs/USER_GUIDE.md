# Literal Memo User Guide

## Getting Started

Literal Memo is a simple note-taking app inspired by [howm](https://kaoriya.github.io/howm/).

**Philosophy**: Write. Throw. Search.

No folders. No tags. No links. Just throw your thoughts into the pile and search when you need them.

## Basic Usage

### Creating a Memo

1. Tap the **+** FAB
2. Write your memo (Markdown supported)
3. Tap the back arrow or navigate back to save

### Editing a Memo

1. Tap a memo from the list — it opens directly in edit mode
2. Edit your content
3. Tap the back arrow to save and sync

Use the **FAB** (eye icon) to switch to preview mode. Tap it again (pencil icon) to return to editing.

### Markdown Toolbar

While editing, a toolbar appears above the keyboard with shortcuts for common Markdown formatting (bold, italic, headers, links, etc.).

### Deleting a Memo

**From the list**: Swipe left on a memo → confirm in the dialog.

**From the edit screen**: Tap the **Delete** button (trash icon, top right) → confirm.

Deleted memos are removed from the app. If Git Sync is enabled, they are moved to the `trash/` folder in your repository — not permanently deleted, just out of the way.

## Searching Memos

Type in the **search bar** at the bottom of the list screen and tap the search key.

- Results show inline snippets with matching text highlighted
- Tap **Back** to clear the search and return to the full list

## Sharing Links

You can share URLs or text from other apps directly to Literal Memo:

1. In your browser or any app, tap **Share**
2. Select **Literal Memo**
3. A new memo is created with the content:
   - URLs become clickable links in Preview mode
   - Text with titles become headed notes

**Example**: Sharing a news article creates:
```markdown
# Article Title

[Article Title](https://example.com/article)
```

This is useful for "read later" workflow: share links on the go, read them on your PC later.

## Git Sync

Supports **GitHub**, **Gitea/Forgejo** (including Codeberg), and any compatible Git forge.

### Setup (GitHub)

1. Create a **private** repository on GitHub (e.g., `username/literalmemo`)
2. Create a Personal Access Token:
   - Go to GitHub → Settings → Developer settings → Personal access tokens
   - Generate a token with `repo` scope
3. In Literal Memo, go to **Settings**
4. Select **GitHub**, enter your token and repository (format: `username/repo`)
5. Tap **Sync**

### Setup (Gitea / Forgejo / Codeberg)

1. Create a **private** repository on your Gitea/Forgejo instance (e.g., Codeberg)
2. Create an access token in your account settings
3. In Literal Memo, go to **Settings**
4. Select **Gitea/Forgejo**, enter the host URL (e.g., `codeberg.org`), token, and repository
5. Tap **Sync**

### How Sync Works

- **Auto-sync**: The app syncs automatically:
  - On app launch
  - When returning to the app (foreground)
  - After saving or deleting a memo
- **Local-first**: Your edits are saved locally first, then synced
- **Remote-authoritative**: After sync, the remote repository has the authoritative version

### Switching Repositories

If you change the forge, host, or repository, all local data is cleared on the next sync. The app will then download everything from the new repository. A warning is shown before this happens.

### Multi-device Usage

When using Literal Memo on multiple devices:

1. **Let the app sync before editing**: The app does this automatically on launch
2. **Don't edit the same memo on two devices simultaneously**
3. **Sync after editing**: The app does this automatically

### Conflict Resolution

If you edit the same memo on two devices before syncing:

- The device that syncs first "wins"
- The other device will download the synced version
- Your unsaved changes on the second device will be overwritten

This is by design. For simple memos, this is usually fine. If you need to recover content, all changes are preserved in Git history.

### Recovery from Git

If you accidentally lose content:

1. Go to your repository
2. Click on the file → History
3. Find the commit with your content
4. Copy or revert as needed

### Neovim Integration

If you use Neovim, you can access your memos directly:
```lua
-- ~/.config/nvim/lua/custom/literalmemo.lua
local M = {}
local pile_path = vim.fn.expand('~/path/to/literalmemo/pile/')

M.new = function()
  local filename = os.date('!%Y%m%d_%H%M%S') .. '.md'
  vim.cmd('edit ' .. pile_path .. filename)
end

M.list = function()
  require('fzf-lua').files({ cwd = pile_path })
end

M.search = function()
  require('fzf-lua').live_grep({ cwd = pile_path })
end

vim.keymap.set('n', '<leader>mn', M.new, { desc = 'New Memo' })
vim.keymap.set('n', '<leader>ml', M.list, { desc = 'List Memo' })
vim.keymap.set('n', '<leader>ms', M.search, { desc = 'Search Memo' })

return M
```

## Troubleshooting

### Sync not working?

- Check your internet connection
- Verify your token has the required scope (`repo` for GitHub, appropriate permissions for Gitea/Forgejo)
- Make sure the repository exists and is accessible
- For Gitea/Forgejo, verify the host URL is correct
- Check Settings to confirm Git Sync is connected

### Memos not appearing on other device?

- Wait a few seconds after launch for sync to complete
- Check if the files exist in your repository
- Try manually triggering sync from Settings

### Lost a memo?

Deleted memos are moved to the `trash/` folder in your repository. To restore one, move the file from `trash/` back to `pile/` directly in your repository, then sync the app.

All changes are also preserved in Git history if you need to recover older content.
