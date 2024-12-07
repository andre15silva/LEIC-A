import RemovalStrategy
import random
from networkx.algorithms.centrality import eigenvector_centrality_numpy


class EigenvectorCentralityStrategy(RemovalStrategy.RemovalStrategy):
    """
    Calculate the biggest K core of a graph and remove one of its nodes
    """
    def __init__(self, G):
        super().__init__(G)
        self.eigenvector_centrality = sorted(eigenvector_centrality_numpy(
            self.G).items(), key=lambda v: v[1])

    def remove_node(self):
        node, _ = self.eigenvector_centrality.pop()
        self.G.remove_node(node)
