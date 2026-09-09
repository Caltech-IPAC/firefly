#!/bin/bash

# ----------------
# install java and return the java command
# if java is already install then just return the java command
# ----------------


# --------------------------
# getJreKey: demine th java platform and return a key to the json file- jreVersion.json
# --------------------------

getJreKey() {
   arch=$(uname -m)
   name=$(uname)
   jarKey="unknown"

   if [[ "$name" == "Darwin" ]]; then
      if [[ "$arch" == "arm64" ]]; then
        jreKey="macOSArm64"
      else
        jreKey="macOSIntel"
      fi
   else
      if [[ "$arch" == "aarch64" ]]; then
        jreKey="linuxArm64"
      elif [[ "$arch" == "x86_64" ]]; then
        jreKey="linux64"
      elif [[ "$arch" == *aarch* ]]; then
        jreKey="linux64"
      elif [[ "$arch" == *arm* ]]; then
        jreKey="linux64"
      elif [[ "$arch" == *amd64* ]]; then
        jreKey="linux64"
      elif [[ "$name" == "Linux" ]]; then
        jreKey="linux64"
      else
        jreKey=
      fi
   fi

   echo "$jreKey"
}


# --------------------------
# define variables
# --------------------------


SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
binDir="${INSTALL_DIR}/bin"
fireflyDir="${HOME}/.firefly"
javaInstallation="${INSTALL_DIR}/javaInstallation"
jreJsonFile="${INSTALL_DIR}/application/current/jreVersion.json"
configJsonFile="$fireflyDir/config.json"
JQ=$(which jq || echo "$binDir/jq")


jreKey=$(getJreKey)

# --------------------------
# look at ~/.firefly/config.json
# if: "java" is not set to "auto" then the value should be a path to a java command and  it overrides the java installation
# else: use the jreVersion.json file to get the firefly installation url and install path
# --------------------------

javaOverride=$($JQ -r ".java" "$configJsonFile")
if [[ $javaOverride != 'auto' && $javaOverride != "" && $javaOverride == *java ]]; then
  JAVA=$javaOverride
else
  jreUrl=$($JQ -r ".$jreKey.url" "$jreJsonFile")
  javaPath=$($JQ -r ".$jreKey.java" "$jreJsonFile")
  JAVA="$javaInstallation/$javaPath"
fi

# --------------------------
# if java already installed then just return the java command
# --------------------------

if [ -f "$JAVA" ]; then
   echo $JAVA
   exit 0
fi

# --------------------------
# an override that isn't a real file, or no download url resolved for this platform
# --------------------------

if [[ $javaOverride != 'auto' && $javaOverride != "" && $javaOverride == *java ]]; then
  echo "The java path configured in $configJsonFile does not exist: $javaOverride" >&2
  exit 1
fi

if [[ "$jreKey" == "unknown" || -z "$jreUrl" || "$jreUrl" == "null" ]]; then
  echo "Unable to determine a Java runtime download for this platform ($(uname) $(uname -m))." >&2
  echo "Set a path to an existing Java installation in $configJsonFile instead of \"auto\"." >&2
  exit 1
fi

# --------------------------
# do the java install installation and return the java command
# --------------------------

# get the JRE and and verify
mkdir -p "$javaInstallation"
curl -fL "$jreUrl" -o "$javaInstallation/jre.tar.gz"
curlStatus=$?
if [ $curlStatus -ne 0 ] || [[ ! -s "$javaInstallation/jre.tar.gz" ]]; then
  echo "Failed to download the Java runtime from $jreUrl" >&2
  exit 1
fi
# expand the JRE and and verify
(cd "$javaInstallation" && tar -xzf jre.tar.gz) &> "$javaInstallation/jre_tar_expand.log"
if [ $? -ne 0 ]; then
  echo "Failed to expand the Java runtime, see $javaInstallation/jre_tar_expand.log" >&2
  exit 1
fi

if [ ! -f "$JAVA" ]; then
  echo "Java executable not found at $JAVA after installing the runtime, see $javaInstallation/jre_tar_expand.log" >&2
  exit 1
fi

# --------------------------
# remove JDK directories from previous versions now that the new one is confirmed working,
# so javaInstallation doesn't accumulate a stale JRE from before an app update bumped it.
# --------------------------

newJdkDir="${javaPath%%/*}"
find "$javaInstallation" -mindepth 1 -maxdepth 1 -type d -name 'jdk-*' ! -name "$newJdkDir" -exec rm -rf {} +

echo $JAVA

