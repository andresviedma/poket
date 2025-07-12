last_version=$(grep -i -E "^poketVersion[[:space:]]*=" gradle.properties | cut -d'=' -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

echo "*** Last version: $last_version"
echo "*** Please enter new version"
read new_version

sed -i -E "/^poketVersion[[:space:]]*=/c\\
poketVersion=$new_version
" gradle.properties

echo "**** updated gradle.properties"
git diff gradle.properties
rm gradle.properties-E

read -p "Are you sure you want to release this version? (y/n)" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    ./gradlew clean publishAllPublicationsToProjectLocalRepository zipMavenCentralPortalPublication releaseMavenCentralPortalPublication

    git commit -m "Release $new_version" gradle.properties
    git tag -a "v$new_version" -m "Version $new_version"
    git push
    git push origin "v$new_version"
fi
