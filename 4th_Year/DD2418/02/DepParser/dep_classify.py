import os
import pickle
from parse_dataset import Dataset
from dep_parser import Parser
from logreg import LogisticRegression


class TreeConstructor:
    """
    This class builds dependency trees and evaluates using unlabeled arc score (UAS) and sentence-level accuracy
    """
    def __init__(self, parser):
        self.__parser = parser

    def build(self, model, words, tags, ds):
        """
        Build the dependency tree using the logistic regression model `model` for the sentence containing
        `words` pos-tagged by `tags`
        
        :param      model:  The logistic regression model
        :param      words:  The words of the sentence
        :param      tags:   The POS-tags for the words of the sentence
        :param      ds:     Training dataset to build datapoints
        """
        # Initialize tree, moves, etc
        moves, i, stack, pred_tree = [], 0, [], [0 for _ in words]

        # Make first move
        x = ds.dp2array(words, tags, i, stack)
        log_probs = model.get_log_probs(x)
        move = log_probs.index(max(log_probs))
        moves += [move]
        i, stack, pred_tree = self.__parser.move(i, stack, pred_tree, move)

        # Make second move
        x = ds.dp2array(words, tags, i, stack)
        log_probs = model.get_log_probs(x)
        move = log_probs.index(max(log_probs))
        moves += [move]
        i, stack, pred_tree = self.__parser.move(i, stack, pred_tree, move)
        
        # Make moves until the end
        while not (len(stack) == 1 and stack[0] == 0 and i >= len(pred_tree)):
            x = ds.dp2array(words, tags, i, stack)
            log_probs = model.get_log_probs(x)
            move = log_probs.index(max(log_probs))
            moves += [move]
            i, stack, pred_tree = self.__parser.move(i, stack, pred_tree, move)
            
        return moves, i, stack, pred_tree

    def evaluate(self, model, test_file, ds):
        """
        Evaluate the model on the test file `test_file` using the feature representation given by the dataset `ds`
        
        :param      model:      The model to be evaluated
        :param      test_file:  The CONLL-U test file
        :param      ds:         Training dataset instance having the feature maps
        """
        p = self.__parser
        test_ds = p.create_dataset(test_file)

        correct_sentences, total_sentences = 0, 0
        correct_arcs, total_arcs = 0, 0
        with open(test_file) as source:
            for words, tags, tree, relations in p.trees(source): 
                moves, i, stack, pred_tree = self.build(model, words, tags, ds)

                # Compute accuracies
                flag = True
                total_sentences += 1
                for predicted, correct in zip(pred_tree, tree):
                    total_arcs += 1
                    if predicted == correct:
                        correct_arcs += 1
                    else:
                        flag = False

                if flag:
                    correct_sentences += 1

        print("Arc accuracy: {0:.2f}%".format(correct_arcs / total_arcs * 100))
        print("Sentence accuracy: {0:.2f}%".format(correct_sentences / total_sentences * 100))


if __name__ == '__main__':
    #
    # TODO:
    # 1) Replace the `create_dataset` function from dep_parser_fix.py to your dep_parser.py file
    # 2) Replace parse_dataset.py with the given new version
    #

    # Create parser
    p = Parser()

    # Create training dataset
    ds = p.create_dataset("en-ud-train-projective.conllu", train=True)

    # Train LR model
    if os.path.exists('model.pkl'):
        # if model exists, load from file
        print("Loading existing model...")
        lr = pickle.load(open('model.pkl', 'rb'))
    else:
        # train model using minibatch GD
        lr = LogisticRegression()
        lr.fit(*ds.to_arrays())
        pickle.dump(lr, open('model.pkl', 'wb'))
    
    # Create test dataset
    test_ds = p.create_dataset("en-ud-dev.conllu")
    # Copy feature maps to ensure that test datapoints are encoded in the same way
    test_ds.copy_feature_maps(ds)
    # Compute move-level accuracy
    lr.classify_datapoints(*test_ds.to_arrays())
    
    # Compute UAS and sentence-level accuracy
    t = TreeConstructor(p)
    t.evaluate(lr, 'en-ud-dev.conllu', ds)
