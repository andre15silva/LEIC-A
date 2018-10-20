#ifndef TASK_MANAGER_H
#define TASK_MANAGER_H


#include <stdio.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include "task.h"
#include "hash_table.h"
#include "linked_list.h"
#include "input.h"


void addTask(HashTable*, NodeList*, char*);
void listDuration(NodeList, char*);
void listDepend(HashTable, NodeList, char*);
void removeTask(HashTable*, NodeList*, char*);
void listPath(HashTable, NodeList);

#endif
