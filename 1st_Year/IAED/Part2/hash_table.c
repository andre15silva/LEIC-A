#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "task.h"
#include "linked_list.h"
#include "hash_table.h"


/* Hash function used by this hash table. */
int hash(TaskKey task, int M) {
    return (task * 2654435761 % 4294967296) % M;
}


/* Function to allocate a new hashtable of size M*/
HashTable newHashTable(int M) {
    int i;
    HashTable new = (HashTable) malloc(sizeof(struct hashtable));

    new->heads = (NodeList*) malloc(M * sizeof(NodeList));
    new->M = M;

    /* Starts every list as NULL */
    for (i = 0; i < M; i++)
        new->heads[i] = NULL;

    return new;
}


/* Function to insert a Task in the given hashtable */
void insertHashTable(HashTable table, Task task) {
    int i;

    i = hash(getTaskKey(task), table->M);
    table->heads[i] = insertEndList(table->heads[i], task);
}


/* Function to delete a task with give TaskKey */
void deleteFromHashTable(HashTable table, TaskKey to_delete) {
    int i;

    i = hash(to_delete, table->M);
    table->heads[i] = deleteFromList(table->heads[i], to_delete);
}


/* Function to search a task with given TaskKey */
Task searchHashTable(HashTable table, TaskKey to_find) {
    int i = hash(to_find, table->M);
    Task found = searchList(table->heads[i], to_find);

    return found;
}


/* Function to deallocate memory allocated for the given HashTable */
void freeHashTable(HashTable table) {
    int i;

    /* Frees all linked lists */
    for (i = 0; i < table->M; i++) {
        freeList(table->heads[i]);
    }

    free(table->heads);
    free(table);
}
