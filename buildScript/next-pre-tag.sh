#!/bin/bash
#
# Tag repository with the next pre tag
#

currBranchLine=$(git status | head -1)
cbAsArray=($currBranchLine)
branch=${cbAsArray[2]}
baseVer='unknown'
#
# Determine the branch, and the version to use for the next tag
# User must be on a rc or dev branch, otherwise exist
#
if [ "$branch" = "dev" ]; then
    cycle=$(git describe --tags --match 'cycle-*' --abbrev=0)
    baseVer=$(echo $cycle | sed 's/cycle-//')
else
    if [[ "$branch" =~ rc- ]]; then
      baseVer=$(echo $branch | sed 's/rc-//')
   else
      echo "Exiting: You must be on a rc or dev branch"
      exit
   fi
fi

#
# get a list of all the previous pre tags and the determine the next number to use
#
matchingPreTags=($(git tag | grep 'pre-.*'${baseVer}))

if [ -z "${matchingPreTags[0]}" ]; then
   nextNum=1
else
   for aTag in "${matchingPreTags[@]}"; do
     asNum=$(echo $aTag $| sed 's/pre-//' | sed 's/-.*$//')
     number_array+=("$asNum")
   done
   sorted_array=($(printf "%s\n" "${number_array[@]}" | sort -n -r))
   nextNum=$(expr ${sorted_array[0]} + 1)
fi

#
# build the new tag string and then "git tag" and "git push --tags"
#
newPreTag=pre-${nextNum}-${baseVer}
read -n1 -s -p  "New ($branch branch) tag will be: $newPreTag, make tag? (y/n [y]): " makeTag
echo
makeTagLower=$(echo "$makeTag" | tr '[:upper:]' '[:lower:]')
if [ "$makeTagLower" = "y" ] || [ "$makeTagLower" = "" ]; then
  echo '>>>>>' git tag $newPreTag
  git tag $newPreTag
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