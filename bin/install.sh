#!/bin/bash

# --------------------------
# install the Firefly server
# --------------------------

defaultInstallRelativePath="firefly"
INSTALL_DIR="$PWD/$defaultInstallRelativePath"
fireflyDir="${HOME}/.firefly"
applicationPath="current"
url=

startScript="ff"
altUrl=
installJre="TRUE"
initialInstall="TRUE"
installType="installing"
doExit="FALSE"
doHelp="FALSE"
confirm="TRUE"
firstInvalid="TRUE"
space="  "
PACKAGE_ASSET_NAME="standalone.zip"
#the following line can be use for testing an alternate asset name
#PACKAGE_ASSET_NAME="test_standalone.zip"

echo "params: " $*


# --------------------------
# define the isTrue function
# --------------------------

isTrue() {
     v=$(echo "$1" | tr '[:upper:]' '[:lower:]')
     if [[ "$v" == "true" || "$v" == "t" ]]; then return 0; else return 1; fi
}

# --------------------------
# checkRequiredCommands: verify the basic tools needed to install are present
# --------------------------

checkRequiredCommands() {
  missing=""
  for cmd in curl unzip realpath; do
    if ! command -v "$cmd" > /dev/null 2>&1; then
      missing="$missing $cmd"
    fi
  done
  if [ -n "$missing" ]; then
    echo "Cannot install: the following required command(s) are missing:$missing"
    echo "Please install them and re-run this script."
    exit 1
  fi
}

# --------------------------
# checkOsCompatibility: warn if the OS does not meet the documented requirements
# (see docs/using-firefly-standalone.md)
# --------------------------

checkOsCompatibility() {
  name=$(uname)
  if [[ "$name" == "Darwin" ]]; then
    osVersion=$(sw_vers -productVersion 2> /dev/null)
    majorVersion=${osVersion%%.*}
    if [[ "$majorVersion" =~ ^[0-9]+$ ]] && [ "$majorVersion" -lt 15 ]; then
      echo "Warning: Firefly requires macOS 15 or greater, detected macOS ${osVersion:-unknown}"
    fi
  elif [[ "$name" == "Linux" ]]; then
    command -v ldconfig > /dev/null 2>&1 && hasLdconfig="TRUE"
    if isTrue $hasLdconfig && ! ldconfig -p 2> /dev/null | grep -q libssl.so.3; then
      echo "Warning: libssl.so.3 was not found. Firefly requires libssl.so.3 (Debian 12+, RHEL 9+, Ubuntu 22.04+, Fedora)."
    fi
  fi
}

checkRequiredCommands
checkOsCompatibility

# --------------------------
# get the parameters
# --------------------------

while [ $# -gt 0 ]; do
  arg="$1"
  if [[ "$arg" == "-url" ]]; then
     shift
     altUrl=$1
  elif [[ "$arg" == "-installDir" ]]; then
     shift
     enteredPath=$1
     mkdir -p "$enteredPath"
     INSTALL_DIR=$(realpath "$1")
  elif [[ "$arg" == "-asUpdate" ]]; then
     installJre="FALSE"
     initialInstall="FALSE"
     applicationPath="new"
     installType="updating"
  elif [[ "$arg" == "-dontConfirm" ]]; then
     confirm="FALSE"
  elif [[ "$arg" == "--help" || "$arg" == "-h" ]]; then
     doHelp="TRUE"
     doExit="TRUE"
  else
     if isTrue $firstInvalid; then
        echo "Invalid arguments passed."
        firstInvalid="FALSE"
     fi
     echo "$space" "invalid argument:" "$arg"
     doHelp="TRUE"
  fi
  shift
done

# --------------------------
# Help on the parameters
# --------------------------

if isTrue $doHelp; then
  echo "Options:"
  echo "$space -url:         the url or the path to the firefly install zip"
  echo "$space -installDir:  the firefly install dir, defaults to ./firefly"
  echo "$space -dontConfirm: the firefly install dir, defaults to ./firefly"
  echo "$space -asUpdate:    stage the install as an auto update"
  echo "$space --help, -h:   this message and exit"
  exit 0;
fi


# --------------------------
# validate and/or override install dir
# --------------------------

if isTrue $confirm && isTrue $initialInstall && [ "$enteredPath" == "" ]; then
   read -p  "Enter installation directory [${defaultInstallRelativePath}]: " enteredPath
   if [ -n "$enteredPath" ]; then
      mkdir -p "$enteredPath"
      INSTALL_DIR=$(realpath "$enteredPath")
   fi
fi


if [ "$INSTALL_DIR" != "" ]; then
  mkdir -p "$INSTALL_DIR"
fi
if [[ ! -w $INSTALL_DIR ]]; then
  echo "Cannot write to the installation dir ${INSTALL_DIR:-$enteredPath}"
  exit 1
fi

# --------------------------
# The install work begin here
# --------------------------

echo "$installType in $INSTALL_DIR"

applicationRoot="${INSTALL_DIR}/application"
applicationDir="${applicationRoot}/${applicationPath}"
binDir="${INSTALL_DIR}/bin"

# --------------------------
# make the directories,
# --------------------------

mkdir -p "$fireflyDir"
mkdir -p "$fireflyDir/server"
mkdir -p "$applicationDir"
mkdir -p "$binDir"
echo "$INSTALL_DIR" > "$fireflyDir/applicationPath.txt"
rm -f "$applicationDir"/complete

# --------------------------
# make the directories,
# --------------------------
JQ=$(which jq)
if [[ "$JQ" == '' ]]; then
  name=$(uname)
  arch=$(uname -m)

  if [[ "$name" == "Darwin" ]]; then
    if [[ "$arch" == "arm64" ]]; then
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-macos-arm64"
    else
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-macos-amd64"
    fi
  elif [[ "$arch" == "x86_64" ]]; then
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-linux-amd64"
  else
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-linux-arm64"
  fi
  echo "installing local jq..."
  if ! curl -fsSL "$jqUrl" -o "$binDir/jq" || [[ ! -s "$binDir/jq" ]]; then
    echo "Failed to download jq from $jqUrl, install failed"
    exit 1
  fi
  chmod +x "$binDir/jq"
  JQ="$binDir/jq"
fi


# --------------------------
# determine the default location of the standalone.jar package
# the default come from a github assert of the current firefly release
# --------------------------

targetPackageFile="${applicationDir}/standalone.zip"

if [ -z "$altUrl" ]; then
  releaseJson=$(curl -s "https://api.github.com/repos/Caltech-IPAC/firefly/releases/latest")
  apiError=$(echo "$releaseJson" | $JQ -r '.message // empty' 2> /dev/null)
  if [ -n "$apiError" ]; then
    echo "Error contacting the GitHub API: $apiError"
    exit 1
  fi
  url=$(echo "$releaseJson" | $JQ -r --arg name "$PACKAGE_ASSET_NAME" '.assets[]? | select(.name == $name) | .browser_download_url')
else
  url=$altUrl
fi

if [ -z "$url" ]; then
  echo "No package defined to download, could not find it as a github asset https://github.com/Caltech-IPAC/firefly/releases"
  exit 1
fi


# --------------------------
# Download or copy the standalone.zip, expand, then expand firefly.war
# --------------------------

echo "install from: $url"
if [[ "$url" == http* ]]; then
   httpStatus=$(curl -sL -w "%{http_code}" "$url" -o "${targetPackageFile}")
   if [[ "$httpStatus" != "200" ]]; then
     echo "Failed to download $url (HTTP status $httpStatus)"
     exit 1
   fi
else
   if [ ! -f "$url" ]; then
     echo "Package file not found: $url"
     exit 1
   fi
   cp "$url" "${targetPackageFile}"
fi
if [[ ! -s "${targetPackageFile}" ]]; then
   echo "Downloaded package is empty: ${targetPackageFile}"
   exit 1
fi

echo "expanding firefly $targetPackageFile..."
(cd "$applicationDir" && unzip -o "${targetPackageFile}" &> "${applicationDir}/standalone-expand.log")
if [ $? -ne 0 ]; then
   echo "Failed to expand $targetPackageFile, see ${applicationDir}/standalone-expand.log"
   exit 1
fi
if [ ! -f "$applicationDir/firefly.war" ]; then
   echo "firefly.war not found after expanding $targetPackageFile, see ${applicationDir}/standalone-expand.log"
   exit 1
fi
mkdir -p "$applicationDir/firefly-war"
echo "expanding firefly.war..."
(cd "$applicationDir/firefly-war" && unzip -o "${applicationDir}/firefly.war" &> "${applicationDir}/war-expand.log")
if [ $? -ne 0 ]; then
   echo "Failed to expand firefly.war, see ${applicationDir}/war-expand.log"
   exit 1
fi

# --------------------------
# make the script executable, put some in correct place
# --------------------------

requiredFiles=("standalone_cleanup.sh" "$startScript" "startFireflyServer.sh" "javaInstaller.sh" "updater.sh")
missingFiles=""
for f in "${requiredFiles[@]}"; do
  if [ ! -f "$applicationDir/$f" ]; then
    missingFiles="$missingFiles $f"
  fi
done
if [ -n "$missingFiles" ]; then
  echo "Expected file(s) missing after expanding the package:$missingFiles"
  exit 1
fi

scriptPath=$(realpath "$0")
cp "$scriptPath" "$applicationDir/install.sh"
chmod 775 "$applicationDir/standalone_cleanup.sh" \
          "$applicationDir/$startScript" \
          "$applicationDir/startFireflyServer.sh" \
          "$applicationDir/javaInstaller.sh" \
          "$applicationDir/updater.sh" \
          "$applicationDir/install.sh"
/bin/mv "$applicationDir/updater.sh" "$applicationRoot"

cp "$applicationDir/$startScript" "$binDir"
chmod +x "$binDir/$startScript"

# --------------------------
# link ff into ~/.local/bin, creating it if needed, so ff is available
# without editing PATH on systems where ~/.local/bin is already on it
# --------------------------

localBinDir="${HOME}/.local/bin"
mkdir -p "$localBinDir"
ln -sf "$binDir/$startScript" "$localBinDir/$startScript"

# --------------------------
# setup default port
# --------------------------


if isTrue $initialInstall; then
   /bin/cp "$applicationDir/default_config.json" "$fireflyDir/config.json"
   if [ ! -f "$fireflyDir/user_ops.sh" ]; then
     echo "JAVA_OPS=" > "$fireflyDir/user_ops.sh"
   fi
fi

# --------------------------
# install java
# --------------------------

if isTrue $installJre; then
  echo "installing java..."
  JAVA=$("$applicationDir"/javaInstaller.sh)
  if [ $? -ne 0 ] || [ -z "$JAVA" ]; then
    echo "Failed to install Java, see error(s) above"
    exit 1
  fi
fi

# --------------------------
# success message
# --------------------------

if isTrue $initialInstall; then
  echo
  echo "Firefly successfully installed, to start Firefly use the $startScript command"
  echo
  echo ">>>>>>>>>>>>>>>>>>>>>> ${binDir#$PWD/}/ff start"
  echo
  echo "You might want to add the bin dir to your PATH: $binDir or ~/.local/bin"
fi


touch "$applicationDir"/complete