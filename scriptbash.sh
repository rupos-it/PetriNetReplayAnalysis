#!/bin/bash

echo "inizio"

directory=../../prom61_origin/ProM/lib/


for file in "$directory"*.jar
do 
  #echo "$file"
  MOVFile=`basename $file`
  ln -s $file $MOVFile
done

echo; echo

exit 0
