#!/bin/sh
MVNW_PATH=".mvn/wrapper"
JAR="$MVNW_PATH/maven-wrapper.jar"
PROPS="$MVNW_PATH/maven-wrapper.properties"
if [ ! -f "$JAR" ]; then
  mkdir -p "$MVNW_PATH"
  curl -fsSL -o "$JAR" https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
  echo "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" > "$PROPS"
fi
exec java -classpath "$JAR" -Dmaven.multiModuleProjectDirectory="$PWD"   -Dmaven.home="$PWD"   org.apache.maven.wrapper.MavenWrapperMain "$@"
