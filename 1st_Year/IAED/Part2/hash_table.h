#ifndef HASH_TABLE_H
#define HASH_TABLE_H

#include <stdio.h>
#include <stdlib.h>
#include "task.h"
#include "linked_list.h"


struct hashtable {
    NodeList *heads;
    int M;
};

typedef struct hashtable* HashTable;


HashTable newHashTable(int);
void insertHashTable(HashTable, Task);
void deleteFromHashTable(HashTable, TaskKey);
Task searchHashTable(HashTable, TaskKey);
void freeHashTable(HashTable);


#endif
