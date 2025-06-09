#!/bin/bash
#
# Tag repository with the next release tag, you must be on a rc branch
#

currBranchLine=$(git status | head -1)
cbAsArray=($currBranchLine)
branch=${cbAsArray[2]}
baseVer='unknown'
#
# Determine the branch, and the version to use for the next tag
# User must be on a rc or dev branch, otherwise exist
#
if [[ "$branch" =~ rc- ]]; then
    baseVer=$(echo $branch | sed 's/rc-//')
else
    echo "Exiting: You must be on a rc branch"
    exit
fi

#
# get a list of all the previous release tags and the determine the next number to use
#
matchingReleaseTags=($(git tag | grep 'release-.*'${baseVer}))

if [ -z "${matchingReleaseTags[0]}" ]; then
   nextRev=0
else
   for aTag in "${matchingReleaseTags[@]}"; do
     sedStr="s/release-${baseVer}.//"
#     echo aTag $aTag
     asNum=$(echo $aTag | sed $sedStr)
     number_array+=("$asNum")
   done
   sorted_array=($(printf "%s\n" "${number_array[@]}" | sort -n -r))
   nextRev=$(expr ${sorted_array[0]} + 1)
fi

#
# build the new tag string and then "git tag" and "git push --tags"
#
newReleaseTag=release-${baseVer}.${nextRev}
dockerTag=${baseVer}.${nextRev}
read -n1 -s -p  "New ($branch branch) tag will be: $newReleaseTag, make tag? (y/n [y]): " makeTag
echo
makeTagLower=$(echo "$makeTag" | tr '[:upper:]' '[:lower:]')
if [ "$makeTagLower" = "y" ] || [ "$makeTagLower" = "" ]; then
  echo '>>>>>' git tag $newReleaseTag
  git tag $newReleaseTag
  echo "docker tags: ${baseVer},${dockerTag},${branch},latest"
  read -n1 -s -p  "push tags (y/n [n]): " doPush
  doPushLower=$(echo "$doPush" | tr '[:upper:]' '[:lower:]')
  echo
  if [ "$doPushLower" = "y" ]; then
      echo '>>>>>' git push origin --tags
      git push origin --tags
  else
      echo not pushing
  fi
else
  echo not taging
fi