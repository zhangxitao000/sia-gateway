#!/bin/sh

# First, find suitable JDK

version=$("java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
jdk_home="no"
if [[ "$version" > "1.8" ]]; then
       jdk_home=${JAVA_HOME}
       echo "default JDK version is OK, JDK home is $jdk_home"
else
      jdk_path=/opt
      echo "begin to find suitable JDK...."
      for path in `find $jdk_path -name jmap`
      do
         _java=${path%/*}/java
         version=$("$_java" -version 2>&1 | awk -F '"' '{print $2}')
         if [[ "$version" > "1.8" ]]; then
             jdk_home=${_java%/bin*}
             echo "find out suitable JDK, JDK home is $jdk_home"
             break
         fi
      done
fi

if [ "$jdk_home" == "no" ] ;then
  echo "no suitable JDK was found, which is required jdk1.8, exit"
  exit 0
fi

JAVA_HOME=$jdk_home
CLASSPATH=.:$JAVA_HOME/lib:$JAVA_HOME/jre/lib
export PATH=$JAVA_HOME/bin:$JAVA_HOME/jre/bin:$PATH
echo "-------------------------java info-------------------------"
echo $(java -version)
echo "-------------------------pwd-------------------------"
echo $(pwd)

# Second, should I watch?
working_directory=$(pwd)
proc_watcher="yes"
if [ "$1" == "--no-watch" ]; then
    proc_watcher="no"
    shift
fi

# Third, choose profile
gateway_config="gateway_service_test.yml"
if [ "$1" == "gateway_service_pro" ]; then
    gateway_config="gateway_service_pro.yml"
fi

echo "using workspace $working_directory"
echo "proc_watch:  $proc_watcher"

javaOpts="-server -Xms256m -Xmx512m -Xss256k -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycleMin=0 -XX:CMSIncrementalDutyCycle=10 -XX:+UseParNewGC -XX:+UseCMSCompactAtFullCollection -XX:-CMSParallelRemarkEnabled -XX:CMSFullGCsBeforeCompaction=0 -XX:CMSInitiatingOccupancyFraction=70 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=."
java $javaOpts -XX:OnOutOfMemoryError='kill -9 %p'  -Dspring.config.location=../config/$gateway_config  -jar  $working_directory/$2

# Fourth, add crontab process watcher
if [ "$proc_watcher" == "yes" ]; then
	sleep 1
	# add crontab
	cronfile=$(pwd)/$1".cron.run"
	crontab -l | grep -v "$1" 1>$cronfile 2>/dev/null
	echo "*/1 * * * * sh $working_directory/gw_proc_watcher.sh \"$1\" \"$working_directory\" \"./run4service.sh --no-watch $1 $2 \"  >/dev/null 2>&1" >> $cronfile
	crontab $cronfile
	rm $cronfile
	exit 0
fi



