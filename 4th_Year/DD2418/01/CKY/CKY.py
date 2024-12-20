from terminaltables import AsciiTable
import argparse

"""
The CKY parsing algorithm.

This file is part of the computer assignments for the course DD2418 Language engineering at KTH.
Created 2019 by Johan Boye.
"""

class CKY :

    # The unary rules as a dictionary from words to non-terminals,
    # e.g. { cuts : [Noun, Verb] }
    unary_rules = {}

    # The binary rules as a dictionary of dictionaries. A rule
    # S->NP,VP would result in the structure:
    # { NP : {VP : [S]}} 
    binary_rules = {}

    # The parsing table
    table = []

    # The backpointers in the parsing table
    backptr = []

    # The words of the input sentence
    words = []


    # Reads the grammar file and initializes the 'unary_rules' and
    # 'binary_rules' dictionaries
    def __init__(self, grammar_file) :
        stream = open( grammar_file, mode='r', encoding='utf8' )
        for line in stream :
            rule = line.split("->")
            left = rule[0].strip()
            right = rule[1].split(',')
            if len(right) == 2 :
                # A binary rule
                first = right[0].strip()
                second = right[1].strip()
                if first in self.binary_rules :
                    first_rules = self.binary_rules[first]
                else :
                    first_rules = {}
                    self.binary_rules[first] = first_rules
                if second in first_rules :
                    second_rules = first_rules[second]
                    if left not in second_rules :
                        second_rules.append[left]
                else :
                    second_rules = [left]
                    first_rules[second] = second_rules
            if len(right) == 1 :
                # A unary rule
                word = right[0].strip()
                if word in self.unary_rules :
                    word_rules = self.unary_rules[word]
                    if left not in word_rules :
                        word_rules.append( left )
                else :
                    word_rules = [left]
                    self.unary_rules[word] = word_rules


    # Parses the sentence a and computes all the cells in the
    # parse table, and all the backpointers in the table
    def parse(self, s) :
        self.words = s.split()        

        self.table = [[[] for i in range(len(self.words))] for i in range(len(self.words))]
        self.backptr = [[[] for i in range(len(self.words))] for i in range(len(self.words))]

        for i, word in enumerate(self.words):
            self.table[i][i] = self.unary_rules[word]
        
        n = len(self.words)
        for i in range(1, n):
            for j in range(i-1, -1, -1):
                for k in range(i-j):
                    for w, lrule in enumerate(self.table[j][j+k]):
                        for z, rrule in enumerate(self.table[j+k+1][i]):
                            if lrule in self.binary_rules and rrule in self.binary_rules[lrule]:
                                self.table[j][i] += self.binary_rules[lrule][rrule]
                                self.backptr[j][i] += [((lrule, j, j+k, w), (rrule, j+k+1, i, z))]


    # Prints the parse table
    def print_table( self ) :
        t = AsciiTable(self.table)
        t.inner_heading_row_border = False
        print( t.table )


    # Prints all parse trees derivable from cell in row 'row' and
    # column 'column', rooted with the symbol 'symbol'
    def print_trees( self, row, column, symbol ) :
        n = len(self.words)-1

        for i, rule in enumerate(self.table[0][n]):
            lrule = self.backptr[0][n][i][0]
            rrule = self.backptr[0][n][i][1]
            output = rule + "("
            output += self.get_partition(lrule) + ","
            output += self.get_partition(rrule)
            output += ")"
            print(output)
        pass

    def get_partition(self, rule):
        symbol = rule[0]
        i = rule[1]
        j = rule[2]
        pos = rule[3]

        output = symbol + "("
        if i == j:
            output += self.words[rule[1]]
        else:
            rule = self.table[i][j][pos]
            if rule == symbol:
                lrule = self.backptr[i][j][pos][0]
                rrule = self.backptr[i][j][pos][1]
                output += self.get_partition(lrule) + ","
                output += self.get_partition(rrule)
        output += ")"
        return output



def main() :

    # Parse command line arguments
    parser = argparse.ArgumentParser(description='CKY parser')
    parser.add_argument('--grammar', '-g', type=str,  required=True, help='The grammar describing legal sentences.')
    parser.add_argument('--input_sentence', '-i', type=str, required=True, help='The sentence to be parsed.')
    parser.add_argument('--print_parsetable', '-pp', action='store_true', help='Print parsetable')
    parser.add_argument('--print_trees', '-pt', action='store_true', help='Print trees')
    parser.add_argument('--symbol', '-s', type=str, default='S', help='Root symbol')

    arguments = parser.parse_args()

    cky = CKY( arguments.grammar )
    cky.parse( arguments.input_sentence )
    if arguments.print_parsetable :
        cky.print_table()
    if arguments.print_trees :
        cky.print_trees( len(cky.words)-1, 0, arguments.symbol )
    

if __name__ == '__main__' :
    main()    
