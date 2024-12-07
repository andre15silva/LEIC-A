import RemovalStrategy
import random
from networkx.algorithms.core import k_core


class KCoreStrategy(RemovalStrategy.RemovalStrategy):
    """
    Calculate the biggest K core of a graph and remove one of its nodes
    """
    def __init__(self, G):
        super().__init__(G)
        self.kcore = list(k_core(self.G))

    def remove_node(self):
        if len(self.kcore) > 0:
            node = random.choice(self.kcore)
            self.G.remove_node(node)
            self.kcore.remove(node)
        else:
            self.kcore = list(k_core(self.G))
            self.remove_node()
