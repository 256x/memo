" literalmemo.vim - Vim integration for Literal Memo
" Place in ~/.vim/plugin/ or add to your vimrc with: source /path/to/literalmemo.vim
"
" Set this to your actual pile directory:
let g:literalmemo_pile = expand('~/path/to/literalmemo/pile/')

function! LiteralMemoNew()
  let l:file = g:literalmemo_pile . strftime('%Y%m%d_%H%M%S') . '.md'
  execute 'edit ' . fnameescape(l:file)
endfunction

function! LiteralMemoList()
  execute 'Explore ' . fnameescape(g:literalmemo_pile)
endfunction

function! LiteralMemoSearch()
  let l:pat = input('Search: ')
  if empty(l:pat) | return | endif
  execute 'vimgrep /' . escape(l:pat, '/') . '/j ' . fnameescape(g:literalmemo_pile) . '**/*.md'
  copen
endfunction

nnoremap <leader>mn :call LiteralMemoNew()<CR>
nnoremap <leader>ml :call LiteralMemoList()<CR>
nnoremap <leader>ms :call LiteralMemoSearch()<CR>
