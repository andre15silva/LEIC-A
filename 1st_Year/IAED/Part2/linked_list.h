#ifndef LINKED_LIST_H
#define LINKED_LIST_H

#include <stdio.h>
#include <stdlib.h>

typedef struct nodeList *NodeList;

#include "task.h"


struct nodeList {
    Task task;
    struct nodeList* prev;
    struct nodeList* next;
};


NodeList newNodeList(Task);
NodeList insertBeginList(NodeList, Task);
NodeList insertEndList(NodeList, Task);
void freeNodeList(NodeList);
void freeList(NodeList);
void freeTasksList(NodeList);
NodeList deleteFromList(NodeList, TaskKey);
Task searchList(NodeList, TaskKey);
void printList(NodeList);
int lenght(NodeList);


#endif
