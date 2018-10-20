#include <stdio.h>
#include <limits.h>
#include <stdlib.h>
#include "task.h"
#include "linked_list.h"


/* Function: newTask
 * -----------------
 * Allocates a new Task with given arguments and returns it
 *
 * ID: new task's ID.
 * description: new task's description.
 * duration: new task's duration.
 * dependencies: new task's vector of dependencies.
 * n_dependencies: number of dependencies of the new task.
 *
 * returns: new task.
 */
Task newTask(unsigned long ID, char *description, unsigned long duration,
            Task* dependencies, unsigned long n_dependencies) {
    Task new = (Task) malloc(sizeof(struct task));

    new->ID = ID;
    new->description = description;
    new->duration = duration;
    new->dependencies = dependencies;
    new->n_dependencies = n_dependencies;
    new->dependents = NULL;
    new->n_dependents = 0;
    new->early_start = 0;
    new->late_start = ULONG_MAX;

    return new;
}


/* Function: getTaskKey
 * --------------------
 * Returns the given task's key
 *
 * task: Task to get the Key from
 *
 * returns: Key correspondent to the given Task
 */
TaskKey getTaskKey(Task task) {
    return task->ID;
}


/* Function: equalTask
 * -------------------
 * Comparator, checks if Tasks are equal
 *
 * task1: one task
 * task2: the other task
 *
 * returns: true if task1's key is equal to task2's key
 */
int equalTask(Task task1, Task task2) {
    return (getTaskKey(task1) == getTaskKey(task2));
}


/* Function: freeTask
 * ------------------
 *  Deallocates memory previosly allocated
 *
 * to_delete: task to free
 *
 * returns: -
 */
void freeTask(Task to_delete) {
    free(to_delete->description);
    freeList(to_delete->dependents);
    free(to_delete->dependencies);
    free(to_delete);
}


/* Function: printTask
 * -------------------
 * Prints given task according to the specification and the logical value of
 * the flag print_flag
 *
 * to_print: task to print
 * print_flag: flag that tells if path is valid
 *
 * returns: -
 */
void printTask(Task to_print, int print_flag) {
    unsigned long i;

    printf("%lu \"%s\" %lu", to_print->ID, to_print->description, to_print->duration);

    if (print_flag) {
        /* Check if task is in critical path
           Print according to specification */
        if (to_print->early_start == to_print->late_start)
            printf(" [%lu CRITICAL]", to_print->early_start);
        else
            printf(" [%lu %lu]", to_print->early_start, to_print->late_start);
    }

    /* Print dependencies */
    for (i = 0; i < to_print->n_dependencies; i++)
        printf(" %lu", to_print->dependencies[i]->ID);

    printf("\n");
}
