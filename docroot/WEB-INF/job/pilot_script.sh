#!/bin/sh 
#
# hostname - portlet pilot script
#

echo "--------------------------------------------------"
echo "hostname job landed on: '"$(hostname)"'"
echo "--------------------------------------------------"
echo "Job execution starts on: '"$(date)"'"

echo "---[WN HOME directory]----------------------------"
ls -l $HOME

echo "---[WN Working directory]-------------------------"
ls -l $(pwd)

echo "---[Your message]---------------------------------"
cat input_file.txt

echo "Job execution ends on: '"$(date)"'"

#
# Following statement simulates a produced job file
#
OUTFILE=job_output.txt
echo "--------------------------------------------------" > $OUTFILE
echo "hostname job landed on: '"$(hostname)"'" >> $OUTFILE
echo "--------------------------------------------------" >> $OUTFILE
cat  input_file.txt >> $OUTFILE

