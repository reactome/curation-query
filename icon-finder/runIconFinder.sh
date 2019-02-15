#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

input_file=$1
## If an input file is not provided, print an error and exit
if [ -z $input_file ]; then
    echo "Usage: $0 [input_file]"
    exit 1;
fi

if [ ! -f $input_file ]; then
    echo "$input_file not found"
    exit 1;
fi

ln -sf $input_file src/main/resources/input.txt

## Make sure the repo is up to date
git pull

## Generate the jar file and run the Icon Finder program
mvn clean package
ln -sf target/icon-finder-1.0-SNAPSHOT-jar-with-dependencies.jar icon-finder.jar
java -jar icon-finder.jar > entities_in_diagrams.txt
