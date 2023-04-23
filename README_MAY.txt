# setup branch
create a new branch with the name of the version that was previously on the main branch
change the branch of the cloned repo with the version that was previously on the main branch to the new branch
clone the repository once more into a folder with the name of the new version and change the alias accordingly

# get appropriate forge mdk
download and extract the forge mdk folder for the wanted version

# install appropriate jdk
open the build.gradle from the new forge mdk folder and check the java version
if there is no jdk for the given version:
install new jdk
change JAVA_HOME in gradlew.bat to the path where the jdk is installed

# adapt forge gradle files
compare the old and new build.gradle and see if any changes need to be made
copy the "gradle" folder from inside the forge mdk folder to this folder

# run gradle scripts
open windows shell in this folder
run ".\gradlew --refresh-dependencies"
run ".\gradlew genEclipseRuns"
run ".\gradlew eclipse"

# create and setup eclipse project
open eclipse and create a new workspace (copy preferences!)
open eclipse and import folder as gradle project
eclipse settings(for workspace):
go to eclipse preferences->Java->Installed JREs and add and select the appropriate jdk (see earlier steps)
go to eclipse preferences->Compiler and set compiler compliance level to 1.8

alternatively eclipse settings(for project):
project properties->Java Build Path->Libraries->JRE System Library->Edit->Select JavaSE-1.8 as execution environment
project properties->Java Compiler->Select JavaSE-1.8

change Java home environment variable in eclipse settings:
Window -> Preferences -> Gradle -> Adanced Options -> Java home
refresh gradle project

# adapt version in mods.toml
compare loaderVersion with that from the mods.toml of the downloaded mdk and adapt
adapt versionRange to new version

# Run/Debug in Eclipse
Run/Debug Configurations -> Java Application -> runClient


Consider: Sync only src folder to git