import RemovalStrategy
import random


"""
Implements the algorithm proposed in:
Zeng, A., & Zhang, C. J. (2013). Ranking spreaders by decomposing complex networks. Physics Letters A, 377(14), 1031-1035.
"""
class MixedDegreeDecompositionStrategy(RemovalStrategy.RemovalStrategy):
    def __init__(self, G, lambd=0.7):
        super().__init__(G)
        self.lambd = lambd
        self.mdd = self._calculate_mdd()

    def _calculate_mdd(self):
        km = {}

        for node in self.G.nodes():
            km[node] = sum([1 for i in self.G.neighbors(node)])

        result = {}
        removed = set()
        while km:
            # Step 2
            M = min(km.values())
            result[M] = []
            to_delete = []
            for node in km:
                if km[node] <= M:
                    result[M].append(node)
                    to_delete.append(node)
            for node in to_delete:
                del km[node]
                removed.add(node)


            # Step 3
            while True:
                # Update km
                for node in km:
                    kr = 0
                    ke = 0
                    for neigh in self.G.neighbors(node):
                        if neigh in removed:
                            ke += 1
                        else:
                            kr += 1
                    km[node] = kr + self.lambd * ke

                # Remove less than M, else break
                if not all(i > M for i in km.values()):
                    to_delete = []
                    for node in km:
                        if km[node] <= M:
                            result[M].append(node)
                            to_delete.append(node)
                    for node in to_delete:
                        del km[node]
                        removed.add(node)

                else:
                    break

        return result

    def remove_node(self):
        k = max(self.mdd)
        biggest_mdd = self.mdd[k]
        node = random.choice(biggest_mdd)
        self.G.remove_node(node)
        biggest_mdd.remove(node)

        if len(biggest_mdd) == 0:
            del self.mdd[k]

