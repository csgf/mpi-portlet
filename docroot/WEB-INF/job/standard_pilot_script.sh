#!/bin/sh 
#
# hostname - portlet pilot script
#
# Hostname Grid job can be considered the equivalent of the 'hello world' code
# of computer programming languages.
#
# The following script does:
#   o The hostname
#   o The execution start/end dates
#   o Listing of the worker node' $HOME directory
#   o Listing of the worker node' $PWD current directory
#   o Shows the input file
#   o Simulates the creation of an output file  

# Get the hostname
HOSTNAME=$(hostname -f)

# In order to avoid concurrent accesses to files, the 
# portlet uses filename prefixes like
# <timestamp>_<username>_filename
# for this reason the file must be located before to use it
INFILE=$(ls -1 | grep input_file.txt)

echo "--------------------------------------------------"
echo "hostname job landed on: '"$HOSTNAME"'"
echo "--------------------------------------------------"
echo "Job execution starts on: '"$(date)"'"

echo "---[WN HOME directory]----------------------------"
ls -l $HOME

echo "---[WN Working directory]-------------------------"
ls -l $(pwd)

echo "---[Your message]---------------------------------"
cat $INFILE

#
# Following statement simulates a produced job file
#
OUTFILE=job_output.txt
echo "--------------------------------------------------" > $OUTFILE
echo "hostname job landed on: '"$HOSTNAME"'" >> $OUTFILE
echo "infile:  '"$INFILE"'"
echo "outfile: '"$OUTFILE"'"
echo "--------------------------------------------------" >> $OUTFILE
cat $INFILE >> $OUTFILE

echo "Job execution ends on: '"$(date)"'"

