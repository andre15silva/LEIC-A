import networkx as nx
import random
from collections import deque
from statistics import mean


class SIR:
    """
    Class for simulating the SIR model:
    
    G - graph to run the simulation on
    beta - probability of an infected node infecting a susceptible node at a given time
    gamma - probability of an infected node becoming recovered at a given time
    p - initial proportion of infected nodes
    c - convergence criterion. the simulation stops if less then c*N nodes are infected
    """
    def __init__(self, G, beta=0.2, gamma=0.4, p=0.05, c=0.005):
        self.G = G
        self.beta = beta
        self.gamma = gamma
        self.p = p
        self.c = c

    """
    Initializes the graph with self.p*N infected nodes
    """

    def setup(self):
        nx.set_node_attributes(self.G, False, "infected")
        nx.set_node_attributes(self.G, False, "recovered")

        n_initially_infected = int(self.p * self.G.number_of_nodes())
        infected_nodes = random.sample(list(self.G.nodes),
                                       n_initially_infected)
        for node in infected_nodes:
            self.G.nodes[node]["infected"] = True

    """
    Computes the proportion of infected nodes
    """

    def infected(self):
        n_infected = len(
            [x for x, y in self.G.nodes(data=True) if y["infected"]])
        return n_infected / self.G.number_of_nodes()

    """
    Computed the proportion of recovered nodes
    """

    def recovered(self):
        n_recovered = len(
            [x for x, y in self.G.nodes(data=True) if y["recovered"]])
        return n_recovered / self.G.number_of_nodes()

    """
    Checks if the simulation has converged
    """

    def converged(self):
        return self.infected() <= self.c

    """
    Main method, runs the SIR simulation
    """

    def run(self):
        self.setup()

        while not self.converged():
            # For each node in the network
            for node in self.G.nodes():
                # If infected, become recovered with probability gamma
                if self.G.nodes[node]["infected"]:
                    if random.uniform(0, 1) < self.gamma:
                        self.G.nodes[node]["infected"] = False
                        self.G.nodes[node]["recovered"] = True
                # If susceptible AND NOT recovered AND connected to an infected node (doesn't matter how many)
                elif not self.G.nodes[node]["recovered"]:
                    for neighbor in self.G.neighbors(node):
                        if self.G.nodes[neighbor]["infected"]:
                            # become infected with probability beta
                            if random.uniform(0, 1) < self.beta:
                                self.G.nodes[node]["infected"] = True
                            break

            print("Infected: " + str(self.infected()))
            print("Recovered: " + str(self.recovered()))

        return self.recovered()
