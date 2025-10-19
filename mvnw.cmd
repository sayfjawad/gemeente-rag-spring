@echo off
setlocal
set MVNW_PATH=.mvn\wrapper
set JAR=%MVNW_PATH%\maven-wrapper.jar
set PROPS=%MVNW_PATH%\maven-wrapper.properties
if not exist %JAR% (
  mkdir %MVNW_PATH%
  powershell -Command "Invoke-WebRequest -Uri https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar -OutFile %JAR%"
  echo distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip > %PROPS%
)
java -classpath %JAR% -Dmaven.multiModuleProjectDirectory=%cd% -Dmaven.home=%cd% org.apache.maven.wrapper.MavenWrapperMain %*
