import networkx as nx
import random
from statistics import mean
from collections import deque


class BFS:
    """
    Implements a much simpler BFS strategy for node infection. Is not supposed to be as accurate
    as other models, but accurate enough where the relation between nodes/infection is still
    perceptible.
    G - graph to run the simulation on
    beta - probability of an infected node infecting a susceptible node at a given time
    p - initial proportion of infected nodes
    """
    def __init__(self, G, beta=0.5, p=0.1):
        self.G = G
        self.beta = beta
        self.p = 0.05

    def setup(self):
        nx.set_node_attributes(self.G, False, "infected")
        n_initially_infected = int(self.p * self.G.number_of_nodes())
        infected_nodes = random.sample(list(self.G.nodes),
                                       n_initially_infected)
        for node in infected_nodes:
            self.G.nodes[node]["infected"] = True

    def infected(self):
        return sum(1 for _, data in self.G.nodes(data=True)
                   if data["infected"]) / self.G.number_of_nodes()

    def run(self):
        print("Running BFS...")
        self.setup()
        infected_start = self.infected()

        queue = deque(node for node, data in self.G.nodes(data=True)
                      if data["infected"])
        already_seen = set()

        while len(queue) > 0:
            node = queue.pop()
            already_seen.add(node)
            for neighbour in self.G.neighbors(node):
                if neighbour not in already_seen and random.uniform(
                        0, 1) < self.beta:
                    self.G.nodes[neighbour]["infected"] = True
                    queue.append(neighbour)

        infected_end = self.infected()
        print(f"Infected start: {infected_start}\tInfected end: {infected_end}")
        return infected_end
