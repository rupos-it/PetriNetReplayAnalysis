#!/bin/bash

echo "inizio"

directory=prom61_origin/ProM/lib/

directory2=BPMNMeasures
directory3=PetriNetReplayAnalysis

for file in "$directory"*.png
do 
  #echo "$file"
  MOVFile=`basename $file`
  rm BPMNMeasures/stdlib/$MOVFile
  ln -s ../../$file BPMNMeasures/stdlib/$MOVFile
  rm PetriNetReplayAnalysis/stdlib/$MOVFile
  ln -s ../../$file PetriNetReplayAnalysis/stdlib/$MOVFile
done

echo; echo

exit 0
