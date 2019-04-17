#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

echo -n "Enter mysql user name: "
read user

echo -n "Enter mysql password: "
# Outputs asterisks instead of plain text
# Taken from https://stackoverflow.com/a/4316755
unset password;
while IFS= read -r -s -n1 pass; do
  if [[ -z $pass ]]; then
     echo
     break
  else
     echo -n '*'
     password+=$pass
  fi
done

echo -n "Enter mysql database name: "
read db

echo -n "Enter mysql host server name (leave blank for localhost): "
read host
if [ -z $host ]; then
    host='localhost'
fi

echo -n "Enter mysql port (leave blank for 3306): "
read port
if [ -z $port ]; then
    port=3306
fi

config_file=config.properties
original_config_file=src/main/resources/$config_file

# Stop git tracking on original configruation file
git update-index --assume-unchanged $original_config_file

# Makes a copy of the sample configuration file
cp -f $original_config_file .

# Removes comments from the original configuration file
sed -i '/^###/d' $config_file

# Replaces the dummy database configuration values
sed -i "s/\(user=\).*/\1$user/" $config_file
sed -i "s/\(pass=\).*/\1$password/" $config_file
sed -i "s/\(host=\).*/\1$host/" $config_file
sed -i "s/\(db=\).*/\1$db/" $config_file
sed -i "s/\(port=\).*/\1$port/" $config_file

# Change permissions to read/write for user only
chmod 600 $config_file
