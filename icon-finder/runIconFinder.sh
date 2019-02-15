#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

## Make sure the repo is up to date
git pull

## Generate the jar file and run the Icon Finder program
mvn clean package
ln -sf target/icon-finder-1.0-SNAPSHOT-jar-with-dependencies.jar icon-finder.jar
java -jar icon-finder.jar > entities_in_diagrams.txt
