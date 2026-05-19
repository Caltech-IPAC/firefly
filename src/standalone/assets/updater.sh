#!/bin/bash

# --------------------------
# do a Firefly server update, an update is installed in the "new" directory
# steps
#   - remove old
#   - mv current to old
#   - mv new to current
#   - execute startFireflyServer.sh
# --------------------------



SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
applicationRoot="${INSTALL_DIR}/application"
appNew="${applicationRoot}/new"
appCurrent="${applicationRoot}/current"
appOld="${applicationRoot}/old"


if [[ -d "$appNew" && -d "$appCurrent"  && -f "$appNew/complete" ]]; then
  /bin/rm -rf "$appOld"
  /bin/mv "$appCurrent" "$appOld"
  /bin/mv "$appNew" "$appCurrent"
fi

exec "$appCurrent/startFireflyServer.sh"

