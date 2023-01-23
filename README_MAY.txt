# get appropriate forge mdk
download the forge mdk folder for the wanted version

# install appropriate jdk
depending on the current forge version, a specific java jdk has to be installed and used
change JAVA_HOME in gradlew.bat to the path where the jdk is installed

# run gradle scripts
open windows shell in this folder
run ".\gradlew genEclipseRuns --refresh-dependencies"
run ".\gradlew eclipse"

# create and setup eclipse project
open eclipse and import folder as gradle project
eclipse settings(for workspace):
go to eclipse preferences->Java->Installed JREs and add and select the jdk1.8
go to eclipse preferences->Compiler and set compiler compliance level to 1.8

alternatively eclipse settings(for project):
project properties->Java Build Path->Libraries->JRE System Library->Edit->Select JavaSE-1.8 as execution environment
project properties->Java Compiler->Select JavaSE-1.8

change Java home environment variable in eclipse settings:
Window -> Preferences -> Gradle -> Adanced Options -> Java home
refresh gradle project


Run/Debug in Eclipse: Run/Debug Configurations -> Java Application -> runClient
Sync only src folder to git