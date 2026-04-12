#!/bin/bash
# sync-memo.sh - sync pile with remote repository
# Usage: ./sync-memo.sh

REPO="$(dirname "$0")/.."
cd "$REPO" || exit 1

git pull

if ! git diff --quiet pile/ || git ls-files --others --exclude-standard pile/ | grep -q .; then
  git add pile/
  git commit -m "sync: $(date -u +%Y%m%d_%H%M%S)"
  git push
else
  echo "Nothing to sync."
fi
