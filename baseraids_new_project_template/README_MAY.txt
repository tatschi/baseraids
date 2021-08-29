install up-to-date jdk version and set JAVA_HOME environment variable to jdk path for eclipse
install jdk1.8
change java home in gradle.properties to jdk1.8 path or remove if that is the JAVA_HOME environment variable is the jdk1.8
open windows shell in this folder
run ".\gradlew genEclipseRuns --refresh-dependencies"
run ".\gradlew eclipse"
open eclipse and import folder as gradle project
eclipse settings(for workspace):
go to eclipse preferences->Java->Installed JREs and add and select the jdk1.8
go to eclipse preferences->Compiler and set compiler compliance level to 1.8
alternatively eclipse settings(for project):
project properties->Java Build Path->Libraries->JRE System Library->Edit->Select JavaSE-1.8 as execution environment
project properties->Java Compiler->Select JavaSE-1.8

Run/Debug in Eclipse: Run/Debug Configurations -> Java Application -> runClient
Sync only src folder to git