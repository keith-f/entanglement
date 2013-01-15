#!/bin/bash

# Generic launcher script
# Author: Keith Flanagan

# The Java class to run
EXE_CLASSNAME="uk.ac.ncl.aries.mongodb.TestGraph2"
RAM="-Xmx1024M"

# Get the filename of this script
SCRIPT_NAME=$0
SCRIPT_PATH=`which $0`
PROG_HOME=`dirname $SCRIPT_PATH`

MVN_OPTS="-o"

MVN="mvn $MVN_OPTS"

if [ ! -d $PROG_HOME/target/dependency ] ; then
    # Install dependencies into the program's target directory if necessary
    ( cd $PROG_HOME ; $MVN dependency:copy-dependencies )
fi

# Configure CLASSPATH
# Include program's compiled classes
CLASSPATH=$CLASSPATH:$PROG_HOME/target/classes

# Include .jar dependencies
for LIB in `find $PROG_HOME/target/dependency -name "*.jar"` ; do
    CLASSPATH=$CLASSPATH:$LIB
done

# Finally, start the application
java $RAM -cp $CLASSPATH $EXE_CLASSNAME $@



