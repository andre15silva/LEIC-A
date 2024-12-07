import os
import time
import argparse
import string
from collections import defaultdict
import numpy as np
from sklearn.neighbors import NearestNeighbors
import random

from tqdm import tqdm


"""
This file is part of the computer assignments for the course DD2418 Language engineering at KTH.
Created 2020 by Dmytro Kalpakchi.
"""


class Word2Vec(object):
    def __init__(self, filenames, dimension=300, window_size=2, nsample=10,
                 learning_rate=0.025, epochs=5, use_corrected=True, use_lr_scheduling=True):
        """
        Constructs a new instance.
        
        :param      filenames:      A list of filenames to be used as the training material
        :param      dimension:      The dimensionality of the word embeddings
        :param      window_size:    The size of the context window
        :param      nsample:        The number of negative samples to be chosen
        :param      learning_rate:  The learning rate
        :param      epochs:         A number of epochs
        :param      use_corrected:  An indicator of whether a corrected unigram distribution should be used
        """
        self.__pad_word = '<pad>'
        self.__sources = filenames
        self.__H = dimension
        self.__lws = window_size
        self.__rws = window_size
        self.__C = self.__lws + self.__rws
        self.__init_lr = learning_rate
        self.__lr = learning_rate
        self.__nsample = nsample
        self.__epochs = epochs
        self.__nbrs = None
        self.__use_corrected = use_corrected
        print(use_corrected)
        self.__use_lr_scheduling = use_lr_scheduling


    def init_params(self, W, w2i, i2w):
        self.__W = W
        self.__w2i = w2i
        self.__i2w = i2w


    @property
    def vocab_size(self):
        return self.__V
        

    def clean_line(self, line):
        """
        The function takes a line from the text file as a string,
        removes all the punctuation and digits from it and returns
        all words in the cleaned line as a list
        
        :param      line:  The line
        :type       line:  str
        """
        line = line.translate(str.maketrans('', '', string.punctuation))
        line = line.translate(str.maketrans('', '', string.digits))
        return line.split()


    def text_gen(self):
        """
        A generator function providing one cleaned line at a time

        This function reads every file from the source files line by
        line and returns a special kind of iterator, called
        generator, returning one cleaned line a time.

        If you are unfamiliar with Python's generators, please read
        more following these links:
        - https://docs.python.org/3/howto/functional.html#generators
        - https://wiki.python.org/moin/Generators
        """
        for fname in self.__sources:
            with open(fname, encoding='utf8', errors='ignore') as f:
                for line in f:
                    yield self.clean_line(line)


    def get_context(self, sent, i):
        """
        Returns the context of the word `sent[i]` as a list of word indices
        
        :param      sent:  The sentence
        :type       sent:  list
        :param      i:     Index of the focus word in the sentence
        :type       i:     int
        """
        context_words = sent[max(0, i-self.__lws):i] + sent[i+1:i+1+self.__rws]
        return list(map(lambda x: self.__w2i[x], context_words))


    def skipgram_data(self):
        """
        A function preparing data for a skipgram word2vec model in 3 stages:
        1) Build the maps between words and indexes and vice versa
        2) Calculate the unigram distribution and corrected unigram distribution
           (the latter according to Mikolov's article)
        3) Return a tuple containing two lists:
            a) list of focus words
            b) list of respective context words
        """
        i = 0
        self.__w2i = {}
        self.__i2w = {}
        self.__unigram_count = {}
        self.__V = 0

        focus_words = []
        context_words = []

        # Generate mappings and compute unigram counts
        for line in self.text_gen():
            for j, word in enumerate(line):
                if word not in self.__w2i:
                    self.__w2i[word] = i
                    self.__i2w[i] = word
                    i += 1
                    self.__unigram_count[word] = 1
                    self.__V += 1
                else:
                    self.__unigram_count[word] += 1

        # Generate result
        for line in self.text_gen():
            for j, word in enumerate(line):
                if word in focus_words:
                    context_words[focus_words.index(word)] += self.get_context(line, j)
                else:
                    focus_words += [word]
                    context_words += [self.get_context(line, j)]

        # Compute unigram distribution and corrected unigram distribution
        self.__unigram_distribution = {}
        self.__corrected_unigram_distribution = {}
        nwords = sum(self.__unigram_count.values())
        for word in self.__unigram_count.keys():
            self.__unigram_distribution[word] = self.__unigram_count[word] / nwords
            self.__corrected_unigram_distribution[word] = self.__unigram_distribution[word] ** 0.75

        return focus_words, context_words


    def sigmoid(self, x):
        """
        Computes a sigmoid function
        """
        return 1 / (1 + np.exp(-x))


    def negative_sampling(self, number, xb, pos):
        """
        Sample a `number` of negatives examples with the words in `xb` and `pos` words being
        in the taboo list, i.e. those should be replaced if sampled.
        
        :param      number:     The number of negative examples to be sampled
        :type       number:     int
        :param      xb:         The index of the current focus word
        :type       xb:         int
        :param      pos:        The index of the current positive example
        :type       pos:        int
        """
        if self.__use_corrected:
            words = list(self.__corrected_unigram_distribution.keys()).copy()
            probabilities = list(self.__corrected_unigram_distribution.values()).copy()
        else:
            words = list(self.__unigram_distribution.keys()).copy()
            probabilities = list(self.__unigram_distribution.values()).copy()

        # Remove focus word from pool
        focus_idx = words.index(self.__i2w[xb])
        del words[focus_idx]
        del probabilities[focus_idx]
        # Remove positive example from pool
        if pos != xb:
            positive_idx = words.index(self.__i2w[pos])
            del words[positive_idx]
            del probabilities[positive_idx]

        # Get number negative examples
        negative_words = random.choices(words, probabilities)[:number]

        return list(map(lambda x: self.__w2i[x], negative_words))


    def train(self):
        """
        Performs the training of the word2vec skip-gram model
        """
        x, t = self.skipgram_data()
        N = len(x)
        print("Dataset contains {} datapoints".format(N))
        
        self.__W = np.random.uniform(size = (N, self.__H))
        self.__U = np.random.uniform(size = (N, self.__H))

        lr = self.__lr
        for ep in range(self.__epochs):
            for i in tqdm(range(N)):

                if self.__use_lr_scheduling:
                    if lr < self.__lr * 0.0001:
                        lr = self.__lr * 0.0001
                    else:
                        lr = self.__lr * (1 - (ep*N+i)/(self.__epochs*N+1))

                for pos_i in t[i]:
                    self.__W[i] -= lr * self.__U[pos_i].dot(self.sigmoid(self.__U[pos_i].T.dot(self.__W[i])) - 1)
                    self.__U[pos_i] -= lr * self.__W[i].dot(self.sigmoid(self.__U[pos_i].T.dot(self.__W[i])) - 1)

                    for neg_i in self.negative_sampling(self.__nsample, i, pos_i):
                        self.__W[i] -= lr * self.__U[neg_i].dot(self.sigmoid(self.__U[neg_i].T.dot(self.__W[i])))
                        self.__U[neg_i] -= lr * self.__W[i].dot(self.sigmoid(self.__U[neg_i].T.dot(self.__W[i])))


    def find_nearest(self, words, metric):
        """
        Function returning k nearest neighbors with distances for each word in `words`
        
        We suggest using nearest neighbors implementation from scikit-learn 
        (https://scikit-learn.org/stable/modules/generated/sklearn.neighbors.NearestNeighbors.html). Check
        carefully their documentation regarding the parameters passed to the algorithm.
    
        To describe how the function operates, imagine you want to find 5 nearest neighbors for the words
        "Harry" and "Potter" using some distance metric `m`. 
        For that you would need to call `self.find_nearest(["Harry", "Potter"], k=5, metric='m')`.
        The output of the function would then be the following list of lists of tuples (LLT)
        (all words and distances are just example values):
    
        [[('Harry', 0.0), ('Hagrid', 0.07), ('Snape', 0.08), ('Dumbledore', 0.08), ('Hermione', 0.09)],
         [('Potter', 0.0), ('quickly', 0.21), ('asked', 0.22), ('lied', 0.23), ('okay', 0.24)]]
        
        The i-th element of the LLT would correspond to k nearest neighbors for the i-th word in the `words`
        list, provided as an argument. Each tuple contains a word and a similarity/distance metric.
        The tuples are sorted either by descending similarity or by ascending distance.
        
        :param      words:   Words for the nearest neighbors to be found
        :type       words:   list
        :param      metric:  The similarity/distance metric
        :type       metric:  string
        """
        # Fit model to the data
        nbrs = NearestNeighbors(n_neighbors=5, metric=metric).fit(self.__W)

        # Get vectors of the query data
        assert(all([x in self.__w2i for x in words]))
        query = np.array(list(map(lambda x: self.__W[x], [self.__w2i[x] for x in words])))

        # Compute nearest neighbors for the query
        distances, indices = nbrs.kneighbors(query)

        result = []
        for i in range(len(words)):
            _result = list(zip(list(map(lambda x: self.__i2w[x], indices[i])), distances[i]))
            result += [_result]
            
        return result


    def write_to_file(self):
        """
        Write the model to a file `w2v.txt`
        """
        try:
            with open("w2v.txt", 'w') as f:
                W = self.__W
                f.write("{} {}\n".format(self.__V, self.__H))
                for i, w in enumerate(self.__w2i):
                    f.write(str(w) + " " + " ".join(map(lambda x: "{0:.6f}".format(x), W[i,:])) + "\n")
        except Exception as e:
            print(e)
            print("Error: failing to write model to the file")


    @classmethod
    def load(cls, fname):
        """
        Load the word2vec model from a file `fname`
        """
        w2v = None
        try:
            with open(fname, 'r') as f:
                V, H = (int(a) for a in next(f).split())
                w2v = cls([], dimension=H)

                W, i2w, w2i = np.zeros((V, H)), [], {}
                for i, line in enumerate(f):
                    parts = line.split()
                    word = parts[0].strip()
                    w2i[word] = i
                    W[i] = list(map(float, parts[1:]))
                    i2w.append(word)

                w2v.init_params(W, w2i, i2w)
        except:
            print("Error: failing to load the model to the file")
        return w2v


    def interact(self):
        """
        Interactive mode allowing a user to enter a number of space-separated words and
        get nearest 5 nearest neighbors for every word in the vector space
        """
        print("PRESS q FOR EXIT")
        text = input('> ')
        while text != 'q':
            text = text.split()
            neighbors = self.find_nearest(text, 'cosine')

            for w, n in zip(text, neighbors):
                print("Neighbors for {}: {}".format(w, n))
            text = input('> ')


    def train_and_persist(self):
        """
        Main function call to train word embeddings and being able to input
        example words interactively
        """
        self.train()
        self.write_to_file()
        self.interact()
        

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='word2vec embeddings toolkit')
    parser.add_argument('-t', '--text', default='harry_potter_1.txt',
                        help='Comma-separated source text files to be trained on')
    parser.add_argument('-s', '--save', default='w2v.txt', help='Filename where word vectors are saved')
    parser.add_argument('-d', '--dimension', default=50, help='Dimensionality of word vectors')
    parser.add_argument('-ws', '--window-size', default=2, help='Context window size')
    parser.add_argument('-neg', '--negative_sample', default=10, help='Number of negative samples')
    parser.add_argument('-lr', '--learning-rate', default=0.025, help='Initial learning rate')
    parser.add_argument('-e', '--epochs', default=5, help='Number of epochs')
    parser.add_argument('-uc', '--use-corrected', action='store_true', default=False,
                        help="""An indicator of whether to use a corrected unigram distribution
                                for negative sampling""")
    parser.add_argument('-ulrs', '--use-learning-rate-scheduling', action='store_true', default=True,
                        help="An indicator of whether using the learning rate scheduling")
    args = parser.parse_args()

    if os.path.exists(args.save):
        w2v = Word2Vec.load(args.save)
        if w2v:
            w2v.interact()
    else:
        w2v = Word2Vec(
            args.text.split(','), dimension=args.dimension, window_size=args.window_size,
            nsample=args.negative_sample, learning_rate=args.learning_rate, epochs=args.epochs,
            use_corrected=args.use_corrected, use_lr_scheduling=args.use_learning_rate_scheduling
        )
        w2v.train_and_persist()
