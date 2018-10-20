#include <stdio.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include "task.h"
#include "hash_table.h"
#include "linked_list.h"
#include "input.h"
#include "task_manager.h"

#define INICIAL_BUFFER_SIZE 128
#define HASHTABLE_SIZE 10007
#define SPACE " "
#define QUOTATION_MARK "\""
#define MAX(a,b) (a > b) ? a : b
#define MIN(a,b) (a < b) ? a : b


/* Flag for printTask, true if path is valid */
int print_flag = 0;


int main() {
    HashTable table = newHashTable(HASHTABLE_SIZE);
    NodeList list = NULL;
    int command = 0;
    char *input;

    while (1) {
        /* Read input and check correctness */
        input = readInput(INICIAL_BUFFER_SIZE);
        command = verifyCommand(input);

        switch (command) {
            case 1:
                addTask(&table, &list, input);
                break;
            case 2:
                listDuration(list, input);
                break;
           case 3:
                listDepend(table, list, input);
                break;
            case 4:
                removeTask(&table, &list, input);
                break;
            case 5:
                listPath(table, list);
                break;
            case 6:
                /* Free hash table, tasks, linkek list and input string */
                freeHashTable(table);
                freeTasksList(list);
                freeList(list);
                free(input);
                return EXIT_SUCCESS;
            default:
                printf("illegal arguments\n");
        }

        free(input);
    }

    return EXIT_SUCCESS;
}


/* Function: addTask
 * -----------------
 * Adds a task with given input to hashtable and linked list
 *
 * table: pointer to the hashtable with the tasks
 * list: pointer to the linked list with the tasks
 * input: string correspondent to the input
 *
 * returns: -
 */
void addTask(HashTable* table, NodeList* list, char *input) {
    Task to_add, *dependencies = NULL, current_task;
    unsigned long i, ID, duration, n_dependencies = 0, current_ID;
    char *description = NULL, *token;

    /* Clear command add */
    token = strtok(input, SPACE);
    token = strtok(NULL, SPACE);

    /* Check if ID already exists */
    ID = strtoul(token, NULL, 10);
    if (searchHashTable((*table), ID) != NULL) {
        printf("id already exists\n");
        return;
    }

    /* Copy description to new string */
    token = strtok(NULL, QUOTATION_MARK);
    description = (char*) realloc(description, sizeof(char) * (strlen(token) + 1));
    strcpy(description, token);

    /* Get duration */
    duration = strtoul(strtok(NULL, SPACE), NULL, 10);

    /* Check if all dependencies exist and add them to dependencies vector */
    while ((token = strtok(NULL, SPACE)) != NULL) {
        current_ID = strtoul(token, NULL, 10);
        current_task = searchHashTable((*table), current_ID);

        if (current_task != NULL) {
            /* Add task to dependencies if it exists */
            n_dependencies += 1;
            dependencies = (Task*) realloc(dependencies, sizeof(Task) * (n_dependencies));
            dependencies[n_dependencies - 1] = current_task;
        } else {
            /* If it doesn't print warning and free */
            printf("no such task\n");
            free(description);
            free(dependencies);
            return;
        }
    }

    /* Path is now invalid */
    print_flag = 0;
    to_add = newTask(ID, description, duration, dependencies, n_dependencies);

    /* Add task to list of depedents of it's depedencies */
    for (i = 0; i < n_dependencies; i++) {
        current_task = dependencies[i];
        current_task->n_dependents += 1;
        current_task->dependents = insertEndList(current_task->dependents, to_add);
    }

    /* Insert new task in hashtable and linked list */
    insertHashTable((*table), to_add);
    (*list) = insertEndList((*list), to_add);
}


/* Function: listDuration
 * ----------------------
 * Lists all tasks whose duration is equal or greater than input
 *
 * head: first node of the list
 * input: string correspondent to the input
 *
 * returns: -
 */
void listDuration(NodeList head, char *input) {
    char *token;
    unsigned long duration;

    /* Clear word duration */
    token = strtok(input, SPACE);
    token = strtok(NULL, SPACE);

    /* If a value was given take that, if not print all tasks */
    if (token == NULL)
        duration = 0;
    else
        duration = strtoul(token, NULL, 10);

    /* Print tasks */
    while (head != NULL) {
        if (head->task->duration >= duration)
            printTask(head->task, print_flag);
        head = head->next;
    }
}


/* Function: listDepend
 * --------------------
 * Prints all task ID's that depend on the task given as input
 *
 * table: hashtable with the tasks
 * list: linked list with the tasks
 * input: string correspondent to the command
 *
 * returns: -
 */
void listDepend(HashTable table, NodeList head, char* input) {
    Task arg;
    NodeList current;
    unsigned long ID, n_dependents = 0;
    char *token;

    /* Clear command depend */
    token = strtok(input, SPACE);
    token = strtok(NULL, SPACE);

    /* Get input ID */
    ID = strtoul(token, NULL, 10);

    if ((arg = searchHashTable(table, ID)) == NULL) {
        /* Print warning if task doesn't exist */
        printf("no such task\n");
    } else {
        printf("%lu:", ID);

        /* Print depedents */
        current = arg->dependents;
        while (current != NULL) {
            printf(" %lu", current->task->ID);
            n_dependents++;
            current = current->next;
        }

        /* If nothing was printed print no dependencies */
        if (n_dependents == 0) {
            printf(" no dependencies");
        }

        printf("\n");
    }
}


/* Function: removeTask
 * --------------------
 * Removes task with given ID
 *
 * table: pointer to the hashtable with the tasks
 * list: pointer to the linked list with the tasks
 * input: string correspondent to the command
 *
 * returns: -
 */
void removeTask(HashTable* table, NodeList *list, char* input) {
    Task to_remove, current_dependency;
    unsigned long ID, i;
    char *token;

    /* Clear command remove */
    token = strtok(input, SPACE);
    token = strtok(NULL, SPACE);

    /* Get ID */
    ID = strtoul(token, NULL, 10);

    /* Get task */
    to_remove = searchHashTable((*table), ID);

    /* Print required warnings*/
    if (to_remove == NULL) {
        printf("no such task\n");
    } else if (to_remove->n_dependents > 0) {
        printf("task with dependencies\n");
    } else {
        /* Remove task from dependents */
        for (i = 0; i < to_remove->n_dependencies; i++) {
            current_dependency = to_remove->dependencies[i];
            current_dependency->n_dependents -= 1;
            current_dependency->dependents = deleteFromList(current_dependency->dependents,
                                                            getTaskKey(to_remove));
        }

        /* path is now invalid */
        print_flag = 0;

        (*list) = deleteFromList((*list), ID);
        deleteFromHashTable((*table), ID);
        freeTask(to_remove);
    }
}


/* Function: calcEarlyStart
 * ------------------------
 * Recursive function to calculate early_start of a given task, and it's dependencies
 *
 * task: task to calculate the early_start of
 *
 * returns: -
 */
void calcEarlyStart(Task task) {
    unsigned long i;
    Task current;

    if (task->n_dependencies == 0) {
        /* terminal case */
        task->early_start = 0;
    } else {
        /* loop dependencies */
        for (i = 0; i < task->n_dependencies; i++) {
            current = task->dependencies[i];

            /* calculate only if necessary */
            if (current->early_start == 0)
                calcEarlyStart(current);

            /* choose maximum value */
            task->early_start = MAX(task->early_start,
                                    current->early_start + current->duration);
        }
    }
}


/* Function: calcLateStart
 * -----------------------
 * Recursive function to calculate late_start of a given task, and it's dependents
 *
 * task: task to calculate the late_start of
 * duration: duration of the project
 *
 * returns: -
 */
void calcLateStart(Task task, unsigned long duration) {
    NodeList head;
    Task current;

    if (task->n_dependents == 0) {
        /* terminal case */
        task->late_start = duration - task->duration;
    } else {
        /* loop depedents */
        for (head = task->dependents; head != NULL; head = head->next) {
                current = head->task;

                /* calculate if necessary */
                if (current->late_start == ULONG_MAX)
                    calcLateStart(current, duration);

                /* choose minimum */
                task->late_start = MIN(task->late_start,
                                        current->late_start - task->duration);
        }
    }
}


/* Function: listPath
 * ------------------
 * Calculates and prints critical path
 *
 * table: hashtable with the tasks
 * list: linked list with the tasks
 *
 * returns: -
 */
void listPath(HashTable table, NodeList list) {
    NodeList head;
    Task current;
    unsigned long duration = 0;

    /* Reset early_start and late_start values */
    head = list;
    while (head != NULL) {
        current = head->task;
        current->early_start = 0;
        current->late_start = ULONG_MAX;
        head = head->next;
    }

    /* Calculate early starts and find out project duration */
    head = list;
    while (head != NULL) {
        current = head->task;
        /* calculate if final task */
        if (current->n_dependents == 0) {
            calcEarlyStart(current);
            duration = MAX(duration, current->early_start + current->duration);
        }
        head = head->next;
    }

    /* Calculate late starts of final tasks */
    head = list;
    while (head != NULL) {
        current = head->task;
        /* calculate if inicial task */
        if (current->n_dependencies == 0)
            calcLateStart(current, duration);
        head = head->next;
    }

    /* Print critical path */
    print_flag = 1;
    head = list;
    while (head != NULL) {
        current = head->task;
        if (current->early_start == current->late_start) {
            printTask(current, print_flag);
        }
        head = head->next;
    }

    printf("project duration = %lu\n", duration);
}
