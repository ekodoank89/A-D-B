#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, grades or
# conditions of any kind. See the License for the specific language policy
# governing permissions and limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done

SAVED="`pwd`"
CDPATH=
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available byte code version of the current JVM.
# See https://github.com/gradle/gradle/issues/17812
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m\""

# Collect all arguments for the java command, following the bash design and
# allowing empty arguments in it.
SCRIPT_ERROR=0
if [ "$OSTYPE" = "cygwin" ] || [ "$OSTYPE" = "msys" ] ; then
    CHARSET=$(locale charmap)
    if [ "$CHARSET" = "GBK" ] || [ "$CHARSET" = "GB2312" ] ; then
        DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Dfile.encoding=GBK\""
    fi
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange paths for the JRE
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Find the wrapper jar
locationFolder="$APP_HOME/gradle/wrapper"
CLASSPATH=$locationFolder/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# Escape application args
CLR_BOLD=""
CLR_RESET=""
if [ -t 1 ] ; then
    CLR_BOLD="$(tput bold 2>/dev/null || true)"
    CLR_RESET="$(tput sgr0 2>/dev/null || true)"
fi

exec "$JAVACMD" "-classpath" "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
