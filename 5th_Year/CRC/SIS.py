import networkx as nx
import random
from collections import deque
from statistics import mean


class SIS:
    """
    Class for simulating the SIS model:
    
    G - graph to run the simulation on
    beta - probability of an infected node infecting a susceptible node at a given time
    gamma - probability of an infected node becoming susceptible at a given time
    p - initial proportion of infected nodes
    c - convergence criteria. the simulation stops if in the last 10 iterations the proportion of infected nodes remains within an interval given by c
    """
    def __init__(self, G, beta=0.2, gamma=0.4, p=0.05, c=0.1):
        self.G = G
        self.beta = beta
        self.gamma = gamma
        self.p = p
        self.c = c
        self.past = deque(maxlen=10)

    """
    Initializes the graph with self.p*N infected nodes
    """

    def setup(self):
        nx.set_node_attributes(self.G, False, "infected")
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
    Checks if the simulation has converged
    """

    def converged(self, infected):
        # Discards last if full
        if len(self.past) == 10:
            self.past.popleft()

        # Insert new time
        self.past.append(infected / self.G.number_of_nodes())

        if len(self.past) == 10:
            max_ = max(self.past)
            min_ = min(self.past)
            return (max_ - min_) < self.c
        else:
            return False

    """
    Main method, runs the SIS simulation
    """

    def run(self):
        print("Running SIS...")
        self.setup()

        infected = len([x for x, y in self.G.nodes(data=True) if y["infected"]])
        while not self.converged(infected):
            # For each node in the network
            for node in self.G.nodes():
                # If infected, become susceptible with probability gamma
                if self.G.nodes[node]["infected"]:
                    if random.uniform(0, 1) < self.gamma:
                        self.G.nodes[node]["infected"] = False
                        infected -= 1
                # If susceptible AND connected to an infected node (doesn't matter how many)
                else:
                    for neighbor in self.G.neighbors(node):
                        if self.G.nodes[neighbor]["infected"]:
                            # become infected with probability beta
                            if random.uniform(0, 1) < self.beta:
                                self.G.nodes[node]["infected"] = True
                                infected += 1
                            break

        print("Infected: " + str(mean(self.past)))
        return mean(self.past)
