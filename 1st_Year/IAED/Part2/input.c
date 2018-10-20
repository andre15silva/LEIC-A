#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "input.h"

#define SPACE " "
#define QUOTATION_MARK "\""


/* Function: readInput
 * -------------------
 * Reads variable size input from stdin and returns it
 *
 * inicial_size: inicial buffer size
 *
 * returns: string correspondent to the input
 */
char* readInput(size_t inicial_size) {
    /* Allocate inicial size */
    char *input;
    int c;
    size_t len = 0, size = inicial_size;

    input = (char*) malloc(sizeof(char) * size);

    if (input == NULL)
        return input;

    /* Accept input until new line or EOF */
    while (((c = getchar()) != '\n') && (c != EOF)) {
        input[len++] = (char) c;

        /* realloc if size has been reached */
        if (len == size) {
            size *= 2;
            input = (char*) realloc(input, sizeof(char) * size);

            if (input == NULL)
                return input;
        }
    }

    input[len++] = '\0';

    /* realloc to necessary memory only */
    return (char*) realloc(input, sizeof(char) * len);
}


/* Function: verifyCommand
 * -----------------------
 * Verifies if command is valid
 *
 * input: string correspondent to command to be validated
 *
 * returns: int correspondent to each command, 0 if input is illegal
 */
int verifyCommand(char *input) {
    char *token, *command;

    /* Copy input, because strtok alters string */
    command = (char*) malloc(sizeof(char) * (strlen(input) + 1));
    strcpy(command, input);

    token = strtok(command, SPACE);

    /* Check what command it correspondes to */
    if (token != NULL) {
        if (strcmp(token, "add") == 0 && checkArgumentsAdd()) {
            free(command);
            return 1;
        } else if (strcmp(token, "duration") == 0 && checkArgumentsDuration()) {
            free(command);
            return 2;
        } else if (strcmp(token, "depend") == 0 && checkArgumentsDependRemove()) {
            free(command);
            return 3;
        } else if (strcmp(token, "remove") == 0 && checkArgumentsDependRemove()) {
            free(command);
            return 4;
        } else if (strcmp(token, "path") == 0 && checkArgumentsPathExit()) {
            free(command);
            return 5;
        } else if (strcmp(token, "exit") == 0 && checkArgumentsPathExit()) {
            free(command);
            return 6;
        }
    }

    free(command);
    return 0;
}


/* Function: checkArgumentsAdd
 * ---------------------------
 * Verifies if input is a valid add command
 *
 * returns: 0 if invalid, 1 if valid
 */
int checkArgumentsAdd() {
    char *token;
    long ID, duration;

    token = strtok(NULL, SPACE);
    /* Verify if next argument is an ID */
    if (!(token != NULL && (ID = strtol(token, NULL, 10) > 0 && errno != ERANGE)))
        return 0;

    token = strtok(NULL, QUOTATION_MARK);
    /* Verify if next argument is a description */
    if (!(token != NULL && strlen(token) <= 8000))
        return 0;

    token = strtok(NULL, SPACE);
    /* Verify if next argument is a duration */
    if (!(token != NULL && (duration = strtol(token, NULL, 10)) > 0 && errno != ERANGE))
        return 0;

    token = strtok(NULL, SPACE);
    /* Verify if all remaining arguments are possible dependencies */
    while (token != NULL) {
        if (!((ID = strtoul(token, NULL, 10)) > 0 && errno != ERANGE))
            return 0;
        token = strtok(NULL, SPACE);
    }

    return 1;
}


/* Function: checkArgumentsDuration
 * --------------------------------
 * Verifies if input is a valid duration command
 *
 * returns: 0 if invalid, 1 if valid
 */
int checkArgumentsDuration() {
    char *token;
    long value;

    token = strtok(NULL, SPACE);
    /* Verify if remaining argument is a value */
    if (token == NULL)
        return 1;
    else if ((value = strtol(token, NULL, 10)) > 0 && errno != ERANGE) {
        token = strtok(NULL, SPACE);
        return (token == NULL);
    }

    return 0;
}


/* Function: checkArgumentsDependRemove
 * ------------------------------------
 * Verifies if input is a valid depend or remove command
 *
 * returns: 0 if invalid, 1 if valid
 */
int checkArgumentsDependRemove() {
    char *token;
    long ID;

    token = strtok(NULL, SPACE);
    /* Verify if remaining argument is an ID */
    if (token != NULL && (ID = strtol(token, NULL, 10)) > 0 && errno != ERANGE) {
        token = strtok(NULL, SPACE);
        return (token == NULL);
    }

    return 0;
}


/* Function: checkArgumentsPathExit
 * --------------------------------
 * Verifies if input is a valid path or exit command
 *
 * returns: 0 if invalid, 1 if valid
 */
int checkArgumentsPathExit() {
    char *token = strtok(NULL, SPACE);

    return (token == NULL);
}
