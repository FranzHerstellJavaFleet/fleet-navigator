#!/bin/bash
# Wrapper script for llama-server with correct library path

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set LD_LIBRARY_PATH to include the bin directory
export LD_LIBRARY_PATH="${SCRIPT_DIR}:${LD_LIBRARY_PATH}"

# Execute llama-server with all passed arguments
exec "${SCRIPT_DIR}/llama-server" "$@"
