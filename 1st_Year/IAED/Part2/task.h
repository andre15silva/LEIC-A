#ifndef TASK_H
#define TASK_H

#include <stdio.h>
#include <limits.h>
#include <stdlib.h>

typedef struct task* Task;
typedef unsigned long TaskKey;

#include "linked_list.h"


struct task {
    unsigned long ID;
    char *description;
    unsigned long duration;
    struct task **dependencies;
    unsigned long n_dependencies;
    NodeList dependents;
    unsigned long n_dependents;
    unsigned long early_start;
    unsigned long late_start;
};


Task newTask(unsigned long, char*, unsigned long, Task*, unsigned long);
TaskKey getTaskKey(Task);
int lessTask(Task, Task);
int equalTask(Task, Task);
void freeTask(Task);
void printTask(Task, int);


#endif
