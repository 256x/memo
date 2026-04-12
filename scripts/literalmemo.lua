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
