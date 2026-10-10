#!/bin/sh
set -e
basedir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
wrapper_jar="$basedir/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapper_jar" ]; then
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar" -o "$wrapper_jar"
fi
if [ -n "$JAVA_HOME" ]; then
  java_cmd="$JAVA_HOME/bin/java"
else
  java_cmd="java"
fi
exec "$java_cmd" -classpath "$wrapper_jar" "-Dmaven.multiModuleProjectDirectory=$basedir" org.apache.maven.wrapper.MavenWrapperMain "$@"
