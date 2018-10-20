/*    Project 1   */
/*  Sparse Matrix */
/*   Andre Silva  */
/*      89408     */


/* Libraries */
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>


/* Constants */
#define MAX_FILENAME_SIZE 81
#define MAX_ELEMENTS 10000
#define MAX_COMMAND_SIZE 512
#define MAX_COMPRESS_SIZE 2 * MAX_ELEMENTS


/* Macros */
#define MIN(a,b) (a < b) ? a : b
#define MAX(a,b) (a > b) ? a : b


/* Type definition */
typedef struct {
        unsigned long row, col;
        double value;
} Element;


typedef struct {
        unsigned long min_row, max_row, min_col, max_col;
        int pointer;
        double zero;
        Element elements[MAX_ELEMENTS];
} Matrix;


/* Prototypes */
void add_element(char []);
void print_matrix();
void print_matrix_charac();
void print_rows(char []);
void print_cols(char []);
void sort_matrix(char []);
void change_zero(char []);
void load_matrix();
void write_matrix(char []);
void compress_matrix();


/* Global variables */
Matrix matrix = {ULONG_MAX, 0, ULONG_MAX, 0, 0, 0};
char filename[MAX_FILENAME_SIZE];


/* Main function */
int main(int argc, char *argv[]) {
        char command, args[MAX_COMMAND_SIZE];

        /* Load matrix from file */
        if (argc == 2) {
                strcpy(filename, argv[1]);
                load_matrix();
        }

        while (1) {
                /* Read command */
                command = getchar();
                fgets(args, MAX_COMMAND_SIZE, stdin);

                switch (command) {
                        case 'a':
                                add_element(args);
                                break;
                        case 'p':
                                print_matrix();
                                break;
                        case 'i':
                                print_matrix_charac();
                                break;
                        case 'l':
                                print_rows(args);
                                break;
                        case 'c':
                                print_cols(args);
                                break;
                        case 'o':
                                sort_matrix(args);
                                break;
                        case 'z':
                                change_zero(args);
                                break;
                        case 'w':
                                write_matrix(args);
                                break;
                        case 's':
                                compress_matrix();
                                break;
                        case 'q':
                                return EXIT_SUCCESS;
                        default:
                                printf("Invalid command\n");
                }
        }
}


/* Functions */
void calc_size_dens(unsigned long *size, double *dens) {
        /* Calculates size and density of matrix */
        (*size) = (matrix.max_row - matrix.min_row + 1) * (matrix.max_col - matrix.min_col + 1);
        (*dens) = (((double)matrix.pointer) / (double)(*size)) * 100.0;
}


void copy_matrix(Element vec[]) {
        /* Copies matrix's vector of elements to vec */
        int i;

        for (i = 0; i < matrix.pointer; i++) {
                vec[i] = matrix.elements[i];
        }
}


int less_row(Element a, Element b) {
        /* Checks if Element a's row is smaller than Element b's*/
        return (a.row < b.row);
}


int less_col(Element a, Element b) {
        /* Checks if Element a's col is smaller than Element b's*/
        return (a.col < b.col);
}


void insertion_sort(Element vec[], int size, int (*less)(Element, Element)) {
        /* Insertion Sort algorithm to sort matrix according to function less */
        int i, j;
        Element tmp;

        for (i = 0; i < size; i++) {
                tmp = vec[i];
                j = i;
                while (j > 0 && less(tmp, vec[j - 1])) {
                        vec[j] = vec[j - 1];
                        j--;
                }
                vec[j] = tmp;
        }
}


void update_matrix(Element tested) {
        /* Checks if Element tested changes matrix min/max values and updates*/

        matrix.max_row = MAX(matrix.max_row, tested.row);
        matrix.min_row = MIN(matrix.min_row, tested.row);
        matrix.max_col = MAX(matrix.max_col, tested.col);
        matrix.min_col = MIN(matrix.min_col, tested.col);
}


int search_index(Element target) {
        /* Search matrix for target element, return position or -1 if not found*/
        int i;

        for (i = 0; i < matrix.pointer; i++) {
                if (matrix.elements[i].row == target.row && matrix.elements[i].col == target.col) {
                        return i;
                }
        }

        return -1;
}


void delete_element(int p) {
        /* Deletes element in position p from matrix */
        int i;
        Element deleted = matrix.elements[p];

        /* Shift left elements to the right of the deleted element */
        for (i = p; i < (matrix.pointer - 1); i++) {
                matrix.elements[i] = matrix.elements[i+1];
        }

        matrix.pointer--;

        /* Updates min/max values if deleted element was min or max */
        if (deleted.row == matrix.max_row || deleted.row == matrix.min_row ||
            deleted.col == matrix.max_col || deleted.col == matrix.min_col) {

                matrix.min_row = matrix.min_col = ULONG_MAX;
                matrix.max_row = matrix.max_col = 0;

                for (i = 0; i < matrix.pointer; i++) {
                        update_matrix(matrix.elements[i]);
                }
        }
}


void add_element(char args[]) {
        /* Adds new element to first empty position */
        int p;
        Element new;

        sscanf(args, "%lu %lu %lf", &new.row, &new.col, &new.value);
        /* Checks if element is already represented in matrix */
        p = search_index(new);

        if (p >= 0 && new.value == matrix.zero) {
                /* Deletes element if it existed and new value is equal to the matrix's zero */
                delete_element(p);
        } else if (p >= 0 && new.value != matrix.zero) {
                /* Changes element's value if it existed and new value is not matrix's zero */
                matrix.elements[p] = new;
        } else if (new.value != matrix.zero) {
                /* Adds new element to end of matrix */
                /* and updates matrix's min/max values */
                matrix.elements[matrix.pointer] = new;
                matrix.pointer++;
                update_matrix(new);
        }
}


void print_matrix() {
        /* Prints matrix's elements in the format specified */
        int i;

        if (matrix.pointer == 0) {
                printf("empty matrix\n");
        } else {
                for (i = 0; i < matrix.pointer; i++) {
                        printf("[%lu;%lu]=%.3f\n", matrix.elements[i].row, matrix.elements[i].col,
                                                    matrix.elements[i].value);
                }
        }
}


void print_matrix_charac() {
        /* Prints matrix's characteristics in the format specified */
        unsigned long size;
        double dens;

        if (matrix.pointer == 0) {
                printf("empty matrix\n");
        } else {
                calc_size_dens(&size, &dens);
                printf("[%lu %lu] [%lu %lu] %u / %lu = %.3f%%\n", matrix.min_row, matrix.min_col,
                                                                   matrix.max_row, matrix.max_col,
                                                                   matrix.pointer, size, dens);
        }
}


void print_rows(char args[]) {
        /* Prints selected row in the format specified */
        int i = 0;
        unsigned long row, j;
        Element aux[MAX_ELEMENTS];

        sscanf(args, "%lu", &row);

        if (row <= matrix.max_row && row >= matrix.min_row) {
                /* Sorts matrix by rows, and by cols inside each row */
                copy_matrix(aux);
                insertion_sort(aux, matrix.pointer, less_col);
                insertion_sort(aux, matrix.pointer, less_row);

                /* Searches for first element of the desired row */
                while (aux[i].row != row && i < matrix.pointer)
                        i++;

                if (i < matrix.pointer) {
                        /* Prints values from min_col to max_col if row has at least one element */
                        /* Starts in min_col and goes through row */
                        for (j = matrix.min_col; j <= matrix.max_col; j++) {
                                if (aux[i].col == j && aux[i].row == row) {
                                        printf(" %.3f", aux[i].value);
                                        i++;
                                } else {
                                        printf(" %.3f", matrix.zero);
                                }
                        }
                        printf("\n");
                        return;
                }
        }

        /* Prints empty line if row is out of bounds or has no elements */
        printf("empty line\n");
}


void print_cols(char args[]) {
        /* Prints selected col in the format specified */
        int i = 0;
        unsigned long col, j;
        Element aux[MAX_ELEMENTS];

        sscanf(args, "%lu", &col);

        if (col <= matrix.max_col || col >= matrix.max_col) {
                /* Sorts matrix by col, and by rows inside each col */
                copy_matrix(aux);
                insertion_sort(aux, matrix.pointer, less_row);
                insertion_sort(aux, matrix.pointer, less_col);

                /* Searches for first element of the desired col */
                while (aux[i].col != col && i < matrix.pointer)
                        i++;

                if (i < matrix.pointer) {
                        for (j = matrix.min_row; j <= matrix.max_row; j++) {
                                /* Prints values from min_row to max_row if col has at least one element*/
                                /* Starts in min_row and goes through col */
                                if (aux[i].row == j && aux[i].col == col) {
                                        printf("[%lu;%lu]=%.3f\n", j, aux[i].col, aux[i].value);
                                        i++;
                                } else {
                                        printf("[%lu;%lu]=%.3f\n", j, col, matrix.zero);
                                }
                        }
                        return;
                }
        }

        /* Prints empty column if col is out of bounds or has no elements */
        printf("empty column\n");
}


void sort_matrix(char args[]) {
        /* Sorts matrix according to the input provided */
        char argv[7] = "";

        sscanf(args, "%6s", argv);

        /* Because insertion sort is stable we can sort first by rows and then by cols */
        /* and get our matrix sorted by cols and inside each col by rows */
        /* and vice-versa */
        if (strlen(argv) > 1) {
                insertion_sort(matrix.elements, matrix.pointer, less_row);
                insertion_sort(matrix.elements, matrix.pointer, less_col);
        } else {
                insertion_sort(matrix.elements, matrix.pointer, less_col);
                insertion_sort(matrix.elements, matrix.pointer, less_row);
        }
}


void change_zero(char args[]) {
        /* Changes matrix's zero to input provided */
        int i;
        double new_zero;

        sscanf(args, "%lf", &new_zero);

        matrix.zero = new_zero;

        /* Checks if any element's value is equal to the new zero and deletes it */
        for (i = 0; i < matrix.pointer;) {
                if (matrix.elements[i].value == matrix.zero)
                        delete_element(i);
                else
                        /* Deletion of the element makes elements shift one position to the left */
                        /* so the iterator should only be increased if nothing was deleted */
                        i++;
        }
}


void load_matrix() {
        /* Loads matrix from file */
        /*  Format: <row> <col> <value> */
        Element new;
        FILE *file;

        file = fopen(filename, "r");

        while (fscanf(file, "%lu %lu %lf", &new.row, &new.col, &new.value) == 3) {
                matrix.elements[matrix.pointer] = new;
                matrix.pointer++;
                update_matrix(new);
        }

        fclose(file);
}


void write_matrix(char args[]) {
        /* Writes matrix to file */
        /*  Format: <row> <col> <value> */
        int i;
        FILE *file;

        if (strlen(args) > 1) {
                sscanf(args, "%80s", filename);
        }

        file = fopen(filename, "w+");

        for (i = 0; i < matrix.pointer; i++) {
                fprintf(file, "%lu %lu %f\n", matrix.elements[i].row, matrix.elements[i].col, matrix.elements[i].value);
        }

        fclose(file);
}


void copy_block(Element vec[], Element res[], int l, int r, int n) {
        /* Copies block of elements starting in l position and end on r position */
        /* from vec to res */
        int i, j;

        for (i = l, j = n; i < r; i++, j++)
                res[j] = vec[i];
}


void delete_block(Element vec[], int l, int r, int last) {
        /* Deletes block starting from vector by shifting elements to those positions */
        int i, j;

        for (i = l, j = r; j < last; i++, j++)
                vec[i] = vec[j];
}


void sort_rows_dens(Element res[]) {
        /* Copies matrix sorted by blocks (each block is a row), from highest to lowest density to vector res */
        int i, j, beg = 0, end = 0, current, counter, n = 0;
        int densest_value = 0, last = matrix.pointer;
        Element vec[MAX_ELEMENTS];

        copy_matrix(vec);
        insertion_sort(vec, matrix.pointer, less_col);
        insertion_sort(vec, matrix.pointer, less_row);

        /* Copies aulixiar vector to final vector but sorted by blocks */
        while (last > 0) {
                i = 0;

                /* Searches for highest density block */
                while (i < last) {
                        current = vec[i].row;
                        counter = 0;
                        j = i;

                        /* Searches for bounds of current row */
                        while (j < last && vec[j].row == current) {
                                counter++;
                                j++;
                        }

                        if (counter > densest_value) {
                                beg = i;
                                end = j;
                                densest_value = counter;
                        }

                        i = j;
                }

                /* Copies current densest row to final vector */
                copy_block(vec, res, beg, end, n);
                /*  Deletes that block from auxiliar vector */
                delete_block(vec, beg, end, last);

                n += densest_value;
                last -= densest_value;
                densest_value = 0;
        }
}


int fit(double values[], unsigned long indexes[], Element sorted[], int l, int r) {
        /* Fits row in values and indexes vector */
        /* Row is given by l and r, it's limits on vector sorted */
        int offset = -1, i, j, flag = 1;
        unsigned long current_col;

        /* Searches for smallest offset fit */
        while (flag) {
                offset++;
                current_col = matrix.min_col;
                flag = 0;

                for (i = offset, j = l; j < r; i++, current_col++) {
                        if (current_col == sorted[j].col && values[i] == matrix.zero) {
                                j++;
                        } else if (current_col == sorted[j].col && values[i] != matrix.zero) {
                                flag = 1;
                                break;
                        }
                }
        }

        /* Fits row in the offset found */
        current_col = matrix.min_col;

        for (i = offset, j = l; j < r; i++, current_col++){
                if (current_col == sorted[j].col) {
                        values[i] = sorted[j].value;
                        indexes[i] = sorted[j].row;
                        j++;
                }
        }

        return offset;
}


void compress_matrix() {
        int i, j, counter, offset, largest_offset = 0, last = matrix.pointer;
        unsigned long size, n_print, current, w;
        double dens;
        Element sorted[MAX_ELEMENTS];
        double values[MAX_COMPRESS_SIZE];
        unsigned long indexes[MAX_COMPRESS_SIZE];
        int offsets[matrix.max_row - matrix.min_row + 1];

        calc_size_dens(&size, &dens);

        if (dens > 50.0) {
                printf("dense matrix\n");
        } else {
                sort_rows_dens(sorted);

                /* Zero's resulting vectors */
                /* Zero padding taken care of */
                for (i = 0; i < 2*MAX_ELEMENTS; i++) {
                        values[i] = matrix.zero;
                        indexes[i] = 0;
                }

                for (w = 0; w < (matrix.max_row - matrix.min_row); w++)
                        offsets[w] = 0;

                /* Goes through sorted vector and fits all the rows */
                while (last > 0) {
                        i = 0;
                        current = sorted[i].row;
                        counter = 0;
                        j = i;

                        /* Searches for current row's bounds on sorted vector */
                        while (j < last && sorted[j].row == current) {
                                counter++;
                                j++;
                        }

                        offset = fit(values, indexes, sorted, i, j);

                        largest_offset = MAX(largest_offset, offset);
                        offsets[current - matrix.min_row] = offset;
                        delete_block(sorted, i, j, last);
                        last -= counter;
                }

                /* Number of prints necessary */
                n_print = matrix.max_col - matrix.min_col + largest_offset;

                printf("value =");
                for (i = 0; i <= n_print; i++)
                        printf(" %.3f", values[i]);

                printf("\nindex =");
                for (i = 0; i <= n_print; i++)
                        printf(" %lu", indexes[i]);

                printf("\noffset =");
                for (w = 0; w <= (matrix.max_row - matrix.min_row); w++)
                        printf(" %d", offsets[w]);

                printf("\n");
        }
}
