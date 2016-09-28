# paycheck-parser

## Project description

The project aims to parse paychecks pdf files for grabbing usfeul information for the french administration.
It uses Groovy, and is built with Gradle.

## Usage

    $ ./run.sh -h
    Paycheck Parser for A.F.J.T.
    usage: run.sh [options] [pdf files or folders]
    
    Available options (use -h for help):
     -convertTime   convert <hour>h<minute>min time to fractional hours
     -h,--help  print this message
     -q quiet mode, print only the short file name and the fields value separated by tabs
     -H,--highlight highlight mode. A copy of the parsed pdf is created and the parsed zones are highlighted in yellow
     -vo,--verticalOffset <offset>   adjust vertical coordinates for grabbing text
     -nvo,--negativeVerticalOffset <offset> negative offset adjustment 
