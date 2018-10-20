#ifndef INPUT_H
#define INPUT_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <ctype.h>
#include "hash_table.h"


int verifyCommand(char*);
char* readInput(size_t);
int checkArgumentsAdd();
int checkArgumentsDuration();
int checkArgumentsDependRemove();
int checkArgumentsPathExit();

#endif
