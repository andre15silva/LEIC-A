#!/bin/sh

# This script can be run anywhere if you set the variable below
#
# It runs all the tests with extension .og and creates a folder where all the logs will be stored
# This script has 5 steps:
# 1. Compile the .og file generating an .asm file
# 2. Generate the object file from the .asm file
# 3. Link to generate the executable file from the object file
# 4. Run the program and check if exited correctly
# 5. Check the output of the program with the expected output
#
# It has the following options:
# no-color: print output uncolored
# no-compact: print the output of each test to the console
# no-clean: keep all the logs
#
# Set the path to the folder with the tests
TESTS_PATH="auto-tests"
# Set the path to the folder where the logs are going to be stored
LOG_FOLDER="log"
# Set the path to the compiler (og) already compiled relative to the tests folder
OG_PATH=".."
# Set the path to the folder where the XML is going to be stored
#XML_PATH="xml"
#
# =*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*
# DO NOT TOUCH AFTER THIS LINE
# =*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*

CLEAN=true
COLOR=true

if [ $# -ne 0 ]; then
    for i in $@
    do
        case ${i,,} in
            "no-clean")
                CLEAN=false
                ;;
            "no-color")
                COLOR=false
                ;;
            *)
                echo "Unknown option ${BOLD}$i${RESET}"
                ;;
        esac
    done
fi


cd $TESTS_PATH

if [ "$COLOR" = true ]; then
    RESET="\e[0m"
    BOLD="\e[1m"
    BLUE="\e[34m"
    ORANGE="\e[33m"
    RED="\e[31m"
    GREEN="\e[32m"
fi

if [ ! -d $LOG_FOLDER ]; then
    mkdir $LOG_FOLDER
fi

#if [ ! -d $XML_PATH ]; then
#    mkdir $XML_PATH
#fi

SUCCESS=0
TOTAL=$(ls *.og | wc -l)
CURRENT=0
for file in *.og
do
    CURRENT=$(($CURRENT + 1))
    util=${file%.og}
    echo "FILE: ${file} (${CURRENT}/${TOTAL})"
    echo
    echo > $LOG_FOLDER/${util}.log
 #   echo -n "Generating XML... " | tee $LOG_FOLDER/${util}.log
 #   echo >> $LOG_FOLDER/${util}.log
 #   out=$($OG_PATH/og --target xml $file 2>&1 | tee -a $LOG_FOLDER/${util}.log)
 #   mv ${util}.xml $XML_PATH/
 #   if [ -z "$out" ]; then
 #       echo "DONE"
 #   else
 #       echo "FAILED"
 #       continue
 #   fi


    echo >> $LOG_FOLDER/${util}.log
    echo -n "Generating asm... " | tee -a $LOG_FOLDER/${util}.log
    echo >> $LOG_FOLDER/${util}.log
    out=$($OG_PATH/og -g $file 2>&1 | tee -a $LOG_FOLDER/${util}.log)
    if [ -z "$out" ]; then
        echo "DONE"
    else
        echo "FAILED"
        continue
    fi

    echo >> $LOG_FOLDER/${util}.log
    echo -n "Producing object file... " | tee -a $LOG_FOLDER/${util}.log
    echo >> $LOG_FOLDER/${util}.log
    out=$(yasm -felf32 ${util}.asm 2>&1 | tee -a $LOG_FOLDER/${util}.log)
    if [ -z "$out" ]; then
        echo "DONE"
    else
        echo "FAILED"
        continue
    fi

    echo >> $LOG_FOLDER/${util}.log
    echo -n "Linking file... " | tee -a $LOG_FOLDER/${util}.log
    echo >> $LOG_FOLDER/${util}.log
    out=$(ld -m elf_i386 -o ${util} ${util}.o -lrts 2>&1 | tee -a $LOG_FOLDER/${util}.log)
    if [ -z "$out" ]; then
        echo "DONE"
    else
        echo "FAILED"
        continue
    fi

    echo >> $LOG_FOLDER/${util}.log
    echo -n "Executing program... " | tee -a $LOG_FOLDER/${util}.log
    echo >> $LOG_FOLDER/${util}.log
    rm expected/${util}.mine &> /dev/null
    ./${util} 2>&1 | tee -a expected/${util}.mine $LOG_FOLDER/${util}.log > /dev/null

    if [ $? -eq 0 ]; then
        echo "DONE"
    else
        echo "FAILED"
        continue
    fi

    echo >> $LOG_FOLDER/${util}.log
    echo -n "Comparing outputs... " | tee -a $LOG_FOLDER/${util}.log
    echo >> $LOG_FOLDER/${util}.log

    if [ "$(diff -q -w -b expected/${util}.out expected/${util}.mine)" != "" ]; then
        diff -w -b expected/${util}.out expected/${util}.mine >> $LOG_FOLDER/${util}.log
        echo "FAILED"
        continue
    else
        echo "DONE"
    fi

    echo "Cleaning... "
    rm ${util}.o ${util}.asm expected/${util}.mine ${util}

    if [ "$CLEAN" = true ]; then
        rm $LOG_FOLDER/${util}.log
    fi

    SUCCESS=$(($SUCCESS + 1))
done

echo -e "${BOLD}${BLUE}FINAL RESULTS:${RESET}"
echo -e "${BOLD}Passed: ${GREEN}${SUCCESS}${RESET}"
echo -e "${BOLD}Failed: ${RED}$(($TOTAL - $SUCCESS))${RESET}"

perc=$(echo "scale=2; $SUCCESS * 100 / $TOTAL" | bc -l)

if [ $(echo "$perc > 25" | bc -l) -eq 0 ]; then
    color=$RED
elif [ $(echo "$perc > 50" | bc -l) -eq 0 ]; then
    color=$YELLOW
elif [ $(echo "$perc > 80" | bc -l) -eq 0 ]; then
    color=$ORANGE
else
    color=$GREEN
fi

echo -e "${BOLD}Percentage: ${color}${perc}% ($SUCCESS / $TOTAL)${RESET}"

if [ $SUCCESS -ne $TOTAL ]; then
    echo -e "${BOLD}${ORANGE}NOTE:${RESET} Check ${BOLD}${TESTS_PATH}/${LOG_FOLDER}${RESET} to see the full results"
fi
