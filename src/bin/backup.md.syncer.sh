#!/bin/bash

if [ x"$RHOME" == "x" ]; then
	echo "detect RHOME now ..."
	RHOME=`pwd`/backup
else
	echo "Use RHOME=$RHOME"
fi


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

echo -n "Try to use $RHOME as SYNC Metadata Home."
if [ -d $RHOME ]; then
	echo " ... OK."
else
	echo " ... BAD. Directory not exist!"
	exit
fi

cd $RHOME
for d in `ls $RHOME`; do
	echo -n "Scan Directory '$d' ..."
	if [ -s $d/manifest.desc ]; then
		echo " OK."
		# sync it 
		#scp -r "$RHOME/${d//:/\:}" $RUSER@$RNODE:$RPATH/
		scp -r "$RHOME/${d}" $RUSER@$RNODE:$RPATH/
	else
		echo " ZERO, do not sync it."
		# delete it
		#rm -rf $d
	fi
done
