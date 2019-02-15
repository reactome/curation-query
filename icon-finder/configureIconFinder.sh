#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

echo -n "Enter mysql user name: "
read user

echo -n "Enter mysql password: "
read pass

echo -n "Enter mysql database name: "
read db

echo -n "Enter mysql host server name (leave blank for localhost): "
read host
if [ -z $host ]; then
    host='localhost'
fi

config_file=config.properties
cp -f src/main/resources/$config_file .

# Removes comments from the original configuration file
sed -i '/^###/d' $config_file

# Replaces the dummy database configuration values
sed -i "s/\(user=\).*/\1$user/" $config_file
sed -i "s/\(pass=\).*/\1$pass/" $config_file
sed -i "s/\(host=\).*/\1$host/" $config_file
sed -i "s/\(db=\).*/\1$db/" $config_file

# Reports the contents of the finished configuration file
echo -e "\n"
echo "Icon Finder configured to the following values:"
cat $config_file