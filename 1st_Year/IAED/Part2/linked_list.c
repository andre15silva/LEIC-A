#include <stdio.h>
#include <stdlib.h>
#include "task.h"
#include "linked_list.h"


/* Function: newNodeList
 * ---------------------
 * Aloccates a new Node with given task
 *
 * new_task: task correspondent to the new node
 *
 * returns: new NodeList with the given task
 */
NodeList newNodeList(Task new_task) {
    NodeList new_node = (NodeList) malloc(sizeof(struct nodeList));

    new_node->task = new_task;
    new_node->prev = NULL;
    new_node->next = NULL;

    return new_node;
}


/* Function: insertEndList
 * -----------------------
 * Inserts a given task at the end of the list that starts in head
 *
 * head: first node of the list
 * new_task: task to be inserted in the list
 *
 * returns: new node that is the first of this list
 */
NodeList insertEndList(NodeList head, Task new_task) {
    NodeList to_insert = newNodeList(new_task), tail;

    if (head == NULL) {
        /* Empty list */
        to_insert->prev = to_insert;
        return to_insert;
    } else if (head->prev == head) {
        /* List with one element */
        head->next = to_insert;
        head->prev = to_insert;
        return head;
    } else if (head->prev == NULL) {
        /*  Avoid seg fault, manual tail find */
        tail = head;
        while (tail->next != NULL)
            tail = tail->next;

        tail->next = to_insert;
        head->prev = to_insert;
        return head;
    } else {
        /* Normal insertion */
        tail = head->prev;
        tail->next = to_insert;
        head->prev = to_insert;
        return head;
    }
}


/* Function: freeNodeList
 * ----------------------
 * Deallocates memory previously allocated
 * Frees all the nodes in the given list
 *
 * node: first node of the list
 *
 * returns: -
 */
void freeNodeList(NodeList node) {
    free(node);
}


/* Function: deleteFromList
 * ------------------------
 * Deletes a task with given TaskKey from given list
 *
 * head: first node of the list
 * to_delete: TaskKey correspondent to the task to be deleted
 *
 * returns: node that is the new first element of the list
 */
NodeList deleteFromList(NodeList head, TaskKey to_delete) {
    NodeList t, prev, next;

    for (t = head, prev = NULL; t != NULL; prev = t, t = t->next) {
        if (getTaskKey(t->task) == to_delete) {
            if (t == head && t->next != NULL) {
                /* Task to delete is head */
                t->next->prev = head->prev;
                next = t->next;
                freeNodeList(t);
                return next;
            } else if (t == head && t->next == NULL) {
                /* Task to delete is head, and list has only that element */
                freeNodeList(t);
                return NULL;
            } else if (t->next == NULL) {
                /* Task to delete is last element */
                head->prev = t->prev;
                prev->next = t->next;
                freeNodeList(t);
                return head;
            } else {
                /* Normal deletion */
                prev->next = t->next;
                freeNodeList(t);
                return head;
            }
        }
    }

    return head;
}


/* Function: searcList
 * -------------------
 * Linear searchs linked list starting in head
 *
 * head: first node of the list
 * to_find: TaskKey correspondent to the task to be found
 *
 * returns: the task if found, NULL if not found
 */
Task searchList(NodeList head, TaskKey to_find) {
    /* Go through all nodes */
    while(head != NULL) {
        if (to_find == getTaskKey(head->task))
            return head->task;
        head = head->next;
    }

    return NULL;
}


/* Function: freeList
 * ------------------
 * Deallocates all the nodes of the list
 *
 * head: first node of the list
 *
 * returns: -
 */
void freeList(NodeList head) {
    NodeList t = head, next;

    /* Go through all nodes */
    while (t != NULL) {
        next = t->next;
        freeNodeList(t);
        t = next;
    }
}


/* Function: freeTasksList
 * -----------------------
 * Deallocates all the tasks in the list
 *
 * head: first node of the list
 *
 * returns: -
 */
void freeTasksList(NodeList head) {
    while (head != NULL) {
        freeTask(head->task);
        head = head->next;
    }
}
