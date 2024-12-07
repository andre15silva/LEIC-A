import RemovalStrategy
from networkx.algorithms.core import k_shell
from networkx.algorithms.centrality import eigenvector_centrality_numpy
import random

"""
Implements the Hybrid Centrality as defined in:
Ahajjam, S., & Badir, H. (2018). Identification of influential spreaders in complex networks using HybridRank algorithm. Scientific reports, 8(1), 1-10.
"""
class HybridCentralityStrategy(RemovalStrategy.RemovalStrategy):
    def __init__(self, G):
        super().__init__(G)
        self.kshells = self._calculate_kshells()
        self.icc = self._calculate_icc()
        self.ec = self._calculate_ec()
        self.hc = self._calculate_hc()

    def _calculate_kshells(self):
        kshells = {}
        k = 0
        seen = 0
        while seen < self.G.number_of_nodes():
            kshell_nodes = list(k_shell(self.G, k=k))
            for node in kshell_nodes:
                kshells[node] = k
            seen += len(kshell_nodes)
            k += 1
        return kshells


    def _calculate_icc(self):
        icc = {}
        for node in self.G.nodes():
            icc[node] = 0
            for neigh in self.G.neighbors(node):
                icc[node] += self.kshells[neigh]
        return icc


    def _calculate_ec(self):
        return eigenvector_centrality_numpy(self.G)


    def _calculate_hc(self):
        hc = {}
        for node in self.G.nodes():
            hc[node] = self.icc[node] * self.ec[node]

        return sorted(hc.items(), key=lambda x: x[1])


    def remove_node(self):
        node, hc = self.hc.pop()
        self.G.remove_node(node)
