#!/bin/bash

echo "Do package dist cleans ..."
ant clean
ant clean-test
ant clean-online
ant clean-offline
ant clean-eclipse-files

echo "Clean ivy caches ..."
rm -rf ~/.ivy2/cache/org.datanucleus/datanucleus-*
rm -rf ~/.ivy2/cache/org.apache.hive/hive-*

echo "Do package now ..."
ant package -Doffline=1
ant eclipse-files

