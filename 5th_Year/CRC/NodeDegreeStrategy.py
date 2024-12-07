import RemovalStrategy
import random
import networkx as nx

class NodeDegreeStrategy(RemovalStrategy.RemovalStrategy):
    """
    Sort nodes according to their degrees, in descending order
    """
    def __init__(self, G):
        super().__init__(G)
        self.nodes = sorted(G.degree, key=lambda x: x[1], reverse=True)

    def remove_node(self):
        if len(self.nodes) > 0:
            self.G.remove_node(self.nodes[0][0])
            self.nodes = self.nodes[1:]
