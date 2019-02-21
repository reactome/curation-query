# Icon Finder

This program will take an input text file of Reactome db ids (one per line) for entities that can be represented as icons in pathway diagram objects and output a tab-delimited text file called `entities_in_diagrams.txt`.  The output text file will have the columns: 

* "Entity DB ID"
* "Entity Name"
* "Entity Class"
* "Represented Pathway IDs" (i.e. the db id of the first pathway represented by each diagram in which the entity is found)

## Compiling & Running

The program can be run by invoking the script `runIconFinder.sh` at the root directory of this project.  It requires only one argument which is the path to the input file.  It will prompt for configuration values if none have been previously set, so the configuration documentation below is for reference or if you wish to alter existing configuration values.

Usage: `./runIconFinder.sh input.txt`

NOTE: This script is building and invoking a Java application which requries a Java 8+ environment. You will need maven and a full JDK to compile.

### To run the application manually:

1. Compile the jar file: `mvn clean package`
2. Create a configuration file by following the instruction in the configuration section below.
3. Ensure the input file for the program is located or linked in `src/main/resources/input.txt`.  Alternatively, changed the path for the input_file property in the configuration file.

If the manual compilation was successful, you should see a JAR file in the `target` directory, with a name like `icon-finder-VERSION_NUMBER-jar-with-dependencies.jar`. This is the file you will run with a command like the following:

4. `java -jar target/icon-finder-1.0-SNAPSHOT-jar-with-dependencies.jar [path_to_config_file] > entities_in_diagrams.txt`
 
## Configuration

**NOTE: Configuration is done when calling the main script `runIconFinder.sh` for the first time, so this section is if set values need to be changed and for general reference.**

A auto-configuration script is provided at the root directory of this project and is called `configureIconFinder.sh`.  On first running the Icon Finder tool the configuration script will be run automatically and create a configuration file.   If the configuration needs to be changed, however, this script can be run directly: `./configureIconFinder.sh`.

The configuration file produced by the script will be at the root directory of this project and named `config.properties`.  It can be viewed and edited directly if desired.

A sample configuration file is provided at `src/main/resources/config.properties` and looks like this, but should **NEVER BE EDITED DIRECTLY** and is included in the .gitignore file.

```
user=mysql_user
pass=mysql_pass
db=database_name
host=localhost
port=3306
input_file=src/main/resources/input.txt
```

## Logging
 
When run, the jar file will ouptut log files to a `logs` directory at the root directory of this project.  For each run of the program, two log files will be created:
* a .log file - will contain all statements logged by the program
* a .err file - will contain all statements logged with serverity of WARN, ERROR, or FATAL by the program

The log files will contain timestamps of when the program was executed.
