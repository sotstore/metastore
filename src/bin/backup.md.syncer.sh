#!/bin/bash

HOME=`pwd`/backup

if [ x"$RUSER" == "x" ]; then
	echo "Please set RUSER={remote user name}"
	exit
fi

if [ x"$RNODE" == "x" ]; then
	echo "Please set RNODE={remote backup node}"
	exit
fi

if [ x"$RPATH" == "x" ]; then
	echo "Please set RPATH={remote backup path}"
	exit
fi

echo -n "Try to use $HOME as SYNC Metadata Home."
if [ -d $HOME ]; then
	echo " ... OK."
else
	echo " ... BAD. Directory not exist!"
	exit
fi

cd $HOME
for d in `ls $HOME`; do
	echo -n "Scan Directory '$d' ..."
	if [ -s $d/manifest.desc ]; then
		echo " OK."
		# sync it 
		#scp -r "$HOME/${d//:/\:}" $RUSER@$RNODE:$RPATH/
		scp -r "$HOME/${d}" $RUSER@$RNODE:$RPATH/
	else
		echo " ZERO, do not sync it."
		# delete it
		#rm -rf $d
	fi
done
