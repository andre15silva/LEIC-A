import RemovalStrategy
import random
from networkx.algorithms.centrality import betweenness_centrality


class BetweenessCentralityStrategy(RemovalStrategy.RemovalStrategy):
    """
    Calculate the biggest K core of a graph and remove one of its nodes
    """
    def __init__(self, G):
        super().__init__(G)
        self.betweeness_centrality = sorted(betweenness_centrality(
            self.G, k=10**3, normalized=False).items(),
                                            key=lambda v: v[1])

    def remove_node(self):
        node, _ = self.betweeness_centrality.pop()
        self.G.remove_node(node)
