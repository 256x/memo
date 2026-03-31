# Literal Memo User Guide

## Getting Started

Literal Memo is a simple note-taking app inspired by [howm](https://howm.osdn.jp/).

**Philosophy**: Write. Throw. Search.

No folders. No tags. No links. Just throw your thoughts into the pile and search when you need them.

## Basic Usage

### Creating a Memo

1. Tap the **+** button
2. Write your memo (Markdown supported)
3. Navigate back to save

### Editing a Memo

1. Tap a memo from the list
2. Tap the **Edit** button (pencil icon) or FAB
3. Edit and navigate back to save

### Deleting a Memo

1. Open a memo
2. Tap the **Delete** button (trash icon)
3. Confirm deletion

Deleted memos are moved to trash (not permanently deleted).

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

## GitHub Sync

### Setup

1. Create a **private** repository on GitHub (e.g., `username/literalmemo`)
2. Create a Personal Access Token:
   - Go to GitHub → Settings → Developer settings → Personal access tokens
   - Generate a token with `repo` scope
3. In Literal Memo, go to **Settings**
4. Enter your token and repository (format: `username/repo`)
5. Tap **Sync**

### How Sync Works

- **Auto-sync**: The app syncs automatically:
  - On app launch
  - When returning to the app (foreground)
  - After saving or deleting a memo
- **Local-first**: Your edits are saved locally first, then synced
- **Remote-authoritative**: After sync, GitHub has the authoritative version

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

1. Go to your GitHub repository
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
- Verify your token has `repo` scope
- Make sure the repository exists and is accessible
- Check Settings to confirm GitHub is connected

### Memos not appearing on other device?

- Wait a few seconds after launch for sync to complete
- Check if the files exist in your GitHub repository
- Try manually triggering sync from Settings

### Lost content?

- Check your GitHub repository's commit history
- All changes are preserved in Git
