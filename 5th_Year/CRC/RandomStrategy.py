import networkx as nx
import random
from RemovalStrategy import RemovalStrategy

class RandomStrategy(RemovalStrategy):
    """
    Random node removal strategy

    the logis is simples: every node removel is random
    """
    def remove_node(self):
        n = random.sample(list(self.G.nodes()), 1)[0]
        self.G.remove_node(n)
