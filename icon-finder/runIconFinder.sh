#!/bin/bash
input_file_name=$1
input_file_path=$(readlink -f $input_file_name 2> /dev/null)

CWD=$(pwd) # Current working directory -- from where the script is being called
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

## If an input file is not provided, print an error and exit
if [ -z $input_file_name ]; then
    echo "Usage: $0 [input_file]"
    exit 1;
fi

## If the input file does not exist, print an error and exit
if [ ! -f $input_file_path ]; then
    echo "$input_file_name not found"
    exit 1;
fi

## Make sure the repo is up to date
echo "Updating icon-finder repository from GitHub"
git pull

config_file=config.properties
## If the config file does not exist, run configuration script
if [ ! -f $config_file ]; then
    ./configureIconFinder.sh
fi

## Creates symbolic link to user specified input file so it can be found by the default configuration
ln -sf $(readlink -f $input_file_path) src/main/resources/input.txt

## Generate the jar file and run the Icon Finder program
mvn clean package
ln -sf target/icon-finder-1.0-SNAPSHOT-jar-with-dependencies.jar icon-finder.jar
java -jar icon-finder.jar $config_file > $CWD/entities_in_diagrams.txt
