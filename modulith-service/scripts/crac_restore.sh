#!/bin/bash
# Project CRaC Restore Script

CHECKPOINT_DIR="crac-files"

if [ ! -d "$CHECKPOINT_DIR" ]; then
    echo "Error: Checkpoint directory '$CHECKPOINT_DIR' not found."
    exit 1
fi

echo "Restoring from Checkpoint..."
java -XX:CRaCRestoreFrom="$CHECKPOINT_DIR" \
     -XX:+UseCompactObjectHeaders --enable-preview
