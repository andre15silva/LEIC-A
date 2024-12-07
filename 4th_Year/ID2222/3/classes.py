"""
Paper 

L. De Stefani, A. Epasto, M. Riondato, and E. Upfal, TRIÈST: Counting Local and Global Triangles in Fully-Dynamic Streams with Fixed Memory Size, KDD'16.
http://www.kdd.org/kdd2016/papers/files/rfp0465-de-stefaniA.pdf


Implemented algorithm: FULLY-DYNAMIC

"""

import random as rand
from scipy.special import binom


class FullyDynamic:
    def __init__(self, M):
        self.M = M

        # Dictionnary
        # key node name
        # value set of nodes
        self.S_dict = dict()
        # Set
        # store (u,v) and (v,u)
        self.S_set = set()
        # Dict of local Tau values
        self.Tau_dic = dict()
        self.nbEdges = 0

        self.di = 0
        self.do = 0
        self.t = 0
        self.s = 0

        self.tau = 0

    def sample(self, edge):
        sign = edge[0]
        uv = edge[1]

        self.t += 1
        self.s = self._handleSign(self.s, sign)

        if (sign == "+"):
            if (self._sampleEdge(uv)):
                self._updateCounters(sign, uv)
        else:
            # Sign == "-"

            if (uv in self.S_set):
                self._updateCounters(sign, uv)
                self._removeEdge(uv)
                self.di += 1
            else:
                self.do += 1

    def _handleSign(self, value, sign):
        # Make value = value (sign) 1
        # Sign if operator - or +
        if sign == "+":
            return (value + 1)
        else:
            return (value - 1)

    def _flipBiasedCoin(self, proba):
        return (rand.random() < proba)

    def _addEdge(self, uv):
        u = uv[0]
        v = uv[1]

        self.S_set = self.S_set.union({(u, v), (v, u)})

        if (u in self.S_dict):
            # node u already in the dictionnary
            self.S_dict[u] = self.S_dict[u].union({v})
        else:
            self.S_dict[u] = {v}

        # Same with v
        if (v in self.S_dict):
            self.S_dict[v] = self.S_dict[v].union({u})
        else:
            self.S_dict[v] = {u}

        self.nbEdges += 1

    def _removeEdge(self, uv):
        # Only call for existing connection
        u = uv[0]
        v = uv[1]

        self.S_dict[u] = self.S_dict[u] - {v}
        if (len(self.S_dict[u]) == 0):
            del self.S_dict[u]
        self.S_dict[v] = self.S_dict[v] - {u}
        if (len(self.S_dict[v]) == 0):
            del self.S_dict[v]

        self.S_set = self.S_set - {(u, v), (v, u)}

        self.nbEdges -= 1

    def _sampleEdge(self, uv):
        if (self.do == -self.di):

            if (self.nbEdges < self.M):
                self._addEdge(uv)

                return (True)

            if (self._flipBiasedCoin(self.M / self.t)):
                zw = rand.choice(tuple(self.S_set))

                self._updateCounters("-", zw)
                self._removeEdge(zw)
                self._addEdge(uv)

                return (True)

            ######
            # Unclear in the code
            return (False)
            ######
        if (self._flipBiasedCoin(self.di / (self.di + self.do))):
            self._addEdge(uv)
            self.di -= 1

            return (True)

        self.do -= 1

        return (False)

    def _updateCounters(self, sign, uv):
        u = uv[0]
        v = uv[1]

        Nu = self.S_dict[u] if u in self.S_dict else set()
        Nv = self.S_dict[v] if v in self.S_dict else set()
        Nuv = Nu.intersection(Nv)

        for c in Nuv:
            self.tau = self._handleSign(self.tau, sign)
            self._handleTauValue(u, sign)
            self._handleTauValue(v, sign)
            self._handleTauValue(c, sign)

    def _handleTauValue(self, node, sign):
        if (not (node in self.Tau_dic)):
            self.Tau_dic[node] = 0
        self.Tau_dic[node] = self._handleSign(self.Tau_dic[node], sign)

        if (self.Tau_dic[node] <= 0):
            del self.Tau_dic[node]

    def estimation(self):
        if self.nbEdges < 3:
            return 0
        else:
            w = min(self.M, self.s + self.di + self.do)
            k = 1 - sum([
                binom(self.s, j) * binom(self.di + self.do, w - j) /
                binom(self.s + self.di + self.do, w) for j in range(3)
            ])
            return (self.tau / k) * ((self.s * (self.s - 1) * (self.s - 2)) /
                                     (self.nbEdges * (self.nbEdges - 1) *
                                      (self.nbEdges - 2)))

    def __repr__(self):
        value = "M = {}\n".format(self.M)
        value = value + "Nb edges = {}\n".format(self.nbEdges)
        value = value + "di = {}\n".format(self.di)
        value = value + "do = {}\n".format(self.do)
        value = value + "t = {}\n".format(self.t)
        value = value + "s = {}\n".format(self.s)
        value = value + "tau = {}\n".format(self.tau)
        value = value + "estimation = {}\n".format(self.estimation())

        return (value)
