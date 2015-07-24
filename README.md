# xml-to-html

A configurable parse for converting xml to Watson friendly html. Written for bulk conversion of many xml files with the same general format.


##Requirements

To install this project, you will need:

- [Java 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

###Java 8

1. Download and install the binary relevant to your machine.
2. Edit your `~/.bash_profile` to include the line
```
    export JAVA_HOME=/path/to/java8
```

##Basic Usage

By editing the schema.properties file, you can customize the way that the parser will interpret the xml files.

When running the program, all files in the folder 'xml' will be converted into html and placed into a folder named 'html'. If you have xml files of differing styles/formats each style/format needs to be converted individually.

##schema.properties

The tags that are inspected are shown and commented in this file. Descriptions of each tag are in the file and a few have alternates available in case more than one is needed. In a future update, all properities will be read in as lists to not limit the available options.

##Experimental
This project is evolving very quickly. I am limited by the styles of xml files which are available to me. If you find any for which this program does not work please send them to the email below.

##Maintainer
Andrew Ayres, afayres@us.ibm.com