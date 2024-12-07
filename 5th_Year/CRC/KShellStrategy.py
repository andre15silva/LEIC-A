import RemovalStrategy
from networkx.algorithms.core import k_shell
import random


class KShellStrategy(RemovalStrategy.RemovalStrategy):
    def __init__(self, G):
        super().__init__(G)
        self.kshells = self._calculate_kshells()

    def _calculate_kshells(self):
        kshells = {}
        k = 0
        seen = 0
        while seen < self.G.number_of_nodes():
            kshell_nodes = list(k_shell(self.G, k=k))
            kshells[k] = kshell_nodes
            seen += len(kshell_nodes)
            k += 1
        return kshells

    def remove_node(self):
        k = max(self.kshells)
        biggest_kshell = self.kshells[k]
        node = random.choice(biggest_kshell)
        self.G.remove_node(node)
        biggest_kshell.remove(node)

        if len(biggest_kshell) == 0:
            del self.kshells[k]
