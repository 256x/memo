#!/bin/bash
cd "$(dirname "$0")/../pile" || exit 1
nvim "$(date -u +%Y%m%d_%H%M%S).md"
