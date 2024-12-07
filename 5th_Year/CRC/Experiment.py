import networkx as nx
import random
import numpy as np


class Experiment:
    """
    Class for running an experiment given:

    G - the initial graph
    strategy - node removal strategy
    model - simulation model
    betas - values of beta to use in each of the simulations. defaults to 10 values evenly spaced between 0.0 and 1.0
    removals - percentages of removed nodes to use in each of the simulations. default to 5 values evenly spaced between 0 and 1
    """
    def __init__(self,
                 G,
                 strategy,
                 model,
                 betas=np.linspace(0, 1, num=11),
                 removals=np.linspace(0, 1, num=9)[:-1],
                 iterations=1000):
        self.G = G.copy()
        self.N = self.G.number_of_nodes()
        self.strategy = strategy(self.G)
        self.model = model
        self.betas = betas
        self.removals = removals
        self.iterations = iterations

    """
    Returns the percentage of removed nodes from G, given that N is the initial number of nodes
    """

    def percentage_removed_nodes(self):
        return (self.N - self.G.number_of_nodes()) / self.N

    """
    Runs n iterations according to model on G
    """

    def run_iterations(self, beta):
        print(f"Running for beta={beta}")
        results = []
        for _ in range(self.iterations):
            results += [self.model(self.G, beta=beta).run()]

        return np.mean(results), np.std(results)

    """
    Runs the core logic of the experiment:

    while G has nodes:
        if percentage of removed nodes is larger or equal than the next removal step:
            for each beta:
                run n iterations according to model on G
                store the mean and standard deviation values of the convergence results

        remove node from G according to strategy
    """

    def run(self):
        results = []
        # while G has nodes
        while self.G.number_of_nodes() > 0:
            means = []
            stds = []

            # if percentage of removed nodes is larger or equal than the next removal step
            if len(self.removals) > 0 and self.percentage_removed_nodes() >= self.removals[0]:

                # current removal step, update remaining removal steps
                removal, self.removals = self.removals[0], self.removals[1:]

                # for each beta
                for beta in self.betas:
                    mean, std = self.run_iterations(beta)
                    means += [mean]
                    stds += [std]

                results += [(means, stds)]

            self.strategy.remove_node()

        return results
