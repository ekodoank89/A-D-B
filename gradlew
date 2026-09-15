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
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links - $0 may be a softlink
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
CDPATH= cd "`dirname \"$PRG\"`" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'

# Use the maximum available dir size if HAVE_READLINK is set
if [ -n "$HAVE_READLINK" ]; then
    MAX_FS_SIZE=`df -P . | tail -n 1 | awk '{print $2}'`
    if [ "$MAX_FS_SIZE" -gt 0 ]; then
        DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dorg.gradle.internal.max.directory.size=$MAX_FS_SIZE"
    fi
fi

# Use PSEUDO_TERMINAL if available
if [ -n "$PSEUDO_TERMINAL" ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dorg.gradle.internal.pseudo.terminal=$PSEUDO_TERMINAL"
fi

# Use terminal background color if available
if [ -n "$COLORFGBG" ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dorg.gradle.internal.colorfgbg=$COLORFGBG"
fi

# Use term width if available
if [ -n "$COLUMNS" ]; then
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dorg.gradle.internal.columns=$COLUMNS"
fi

# Warn if non-standard encoding is used
if [ -n "$LANG" ]; then
    case "$LANG" in
        *.UTF-8 | *.utf8 | *.UTF8) ;;
        *)
            DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dfile.encoding=UTF-8"
            ;;
    esac
fi

# Use standard FS encoding for JVM
DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS -Dsun.jnu.encoding=UTF-8"

# Display version if requested
if [ "$1" = "-v" ] || [ "$1" = "--version" ]; then
    echo "Gradle 8.7"
    exit 0
fi

# Find java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses jre/sh/java
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

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Execute Gradle
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
