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
   read -p  "Enter installation directory [${enteredPath:-$defaultInstallRelativePath}]: " enteredPath
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
  if [[ "$name" == "Darwin" ]]; then
    echo jq is is missing from mac os, install failed
    exit 1
  fi
  arch=$(uname -m)

  if [[ "$arch" == "x86_64" ]]; then
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-linux-amd64"
  else
      jqUrl="https://github.com/jqlang/jq/releases/latest/download/jq-linux-arm64"
  fi
  echo "installing local jq..."
  curl -sL "$jqUrl" -o "$binDir/jq"
  chmod +x "$binDir/jq"
  JQ="$binDir/jq"
fi


# --------------------------
# determine the default location of the standalone.jar package
# the default come from a github assert of the current firefly release
# --------------------------

targetPackageFile="${applicationDir}/standalone.zip"

packageUrl=$(curl -s "https://api.github.com/repos/Caltech-IPAC/firefly/releases/latest" | \
$JQ -r '.assets[] | [.name, .browser_download_url] | @tsv' | \
while IFS=$'\t' read -r asset_name download_url; do
  if [ "$asset_name" == $PACKAGE_ASSET_NAME ]; then
    echo "$download_url"
  fi
done)
if [ -z "$altUrl" ]; then
  url=$packageUrl
else
  url=$altUrl
fi

if [ -z "$url" ]; then
  echo "No package defined to download, could not find it as a github asset https://github.com/Caltech-IPAC/firefly/releases"
  exit 0
fi


# --------------------------
# Download or copy the standalone.zip, expand, then expand firefly.war
# --------------------------

echo "install from: $url"
if [[ "$url" == http* ]]; then
   curl -sL "$url" > "${targetPackageFile}"
else
   cp "$url" "${targetPackageFile}"
fi
echo "expanding firefly $targetPackageFile..."
(cd "$applicationDir" && unzip -o "${targetPackageFile}" &> "${applicationDir}/standalone-expand.log")
mkdir -p "$applicationDir/firefly-war"
echo "expanding firefly.war..."
(cd "$applicationDir/firefly-war" && unzip -o "${applicationDir}/firefly.war" &> "${applicationDir}/war-expand.log")

# --------------------------
# make the script executable, put some in correct place
# --------------------------

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
  echo "You might want to add the bin dir to your PATH: $binDir"
fi


touch "$applicationDir"/complete