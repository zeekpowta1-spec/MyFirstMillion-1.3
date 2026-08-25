#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set. Please install a JDK and set JAVA_HOME."
  exit 1
fi
if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "gradle-wrapper.jar is missing."
  echo "Run: gradle wrapper --gradle-version 8.10.2"
  exit 1
fi
exec "$JAVA_HOME/bin/java" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
