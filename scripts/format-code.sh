#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
formatter_home="${HOTELYA_IDEA_HOME:-$HOME/Applications/IntelliJ IDEA.app}"
formatter="$formatter_home/Contents/bin/format.sh"
if [[ ! -x "$formatter" ]]; then
  echo 'Configura HOTELYA_IDEA_HOME con la ruta de IntelliJ IDEA.' >&2
  exit 1
fi
options=(-s "$PWD/config/intellij-code-style.xml" -r -m '*.java,*.kts')
if [[ "${1:-}" == '--check' ]]; then
  options+=(-d)
elif [[ $# -ne 0 ]]; then
  echo 'Uso: ./scripts/format-code.sh [--check]' >&2
  exit 1
fi
formatter_temp=$(mktemp -d "${TMPDIR:-/tmp}/hotelya-format.XXXXXX")
trap 'rm -rf "$formatter_temp"' EXIT
cat > "$formatter_temp/idea.properties" <<PROPERTIES
idea.config.path=$formatter_temp/config
idea.system.path=$formatter_temp/system
idea.log.path=$formatter_temp/log
PROPERTIES
IDEA_PROPERTIES="$formatter_temp/idea.properties" "$formatter" "${options[@]}" src build.gradle.kts settings.gradle.kts
