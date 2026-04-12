#!/bin/bash
# search-memo.sh - search memos with fzf
# Usage: ./search-memo.sh [query]
#
# Requires: fzf
# Opens the selected memo in $EDITOR (fallback: vim)

PILE="$(dirname "$0")/../pile"
cd "$PILE" || exit 1

if [ -n "$1" ]; then
  # Filter by content first, then pick with fzf
  selected=$(grep -rl "$1" . | sed 's|^\./||' | fzf --query="$1" --preview "grep -n '$1' {}")
else
  # Browse all memos with preview
  selected=$(ls -t ./*.md 2>/dev/null | sed 's|^\./||' | fzf --preview 'cat {}' --preview-window=right:60%)
fi

[ -n "$selected" ] && ${EDITOR:-vim} "$selected"
