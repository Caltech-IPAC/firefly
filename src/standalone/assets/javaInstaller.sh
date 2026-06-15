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
# do the java install installation and return the java command
# --------------------------

mkdir "$javaInstallation"
curl -L "$jreUrl" > "$javaInstallation/jre.tar.gz"
(cd "$javaInstallation" &&  tar -xzvf jre.tar.gz &> $javaInstallation/jre_tar_expand.log)
echo $JAVA

