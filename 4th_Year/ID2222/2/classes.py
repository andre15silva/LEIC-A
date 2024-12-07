from functools import reduce
from itertools import combinations


class FrequentItemsets:
    def __init__(self, baskets, s=0.01):
        self.baskets = baskets
        self.s = s * len(baskets)
        self.frequentItemsets = self._findFrequentItemsets()

    def _generateItemsetCounts(self, itemsets):
        result = [0 for i in itemsets]
        for i, itemset in enumerate(itemsets):
            for basket in self.baskets:
                if itemset.issubset(basket):
                    result[i] += 1
        return result

    def _filterItemsets(self, itemsets, counts):
        filteredItemsets = []
        for i, result in enumerate(counts):
            if result >= self.s:
                filteredItemsets += [itemsets[i]]
        return filteredItemsets

    def _findFrequentItemsets(self):
        frequentItemsets = []
        for i in range(max([len(basket) for basket in self.baskets])):
            items = [
                set([a]) for a in reduce(lambda a, b: a.union(b), self.baskets)
            ]
            if i == 0:
                frequentItemsets += [
                    self._filterItemsets(items,
                                         self._generateItemsetCounts(items))
                ]
            else:
                # Generate all possible combinations of itemsets + frequent items
                tempItemsets = []
                for s1 in frequentItemsets[i - 1]:
                    for s2 in frequentItemsets[0]:
                        if len(s1.union(s2)) == i + 1 and s1.union(
                                s2) not in tempItemsets:
                            tempItemsets += [s1.union(s2)]

                # Filter out those who have non-frequent subsets
                kItemsets = []
                for s1 in tempItemsets:
                    passes = True
                    # No need to check j=1 as all 1-itemsets here are frequent
                    for j in range(2, i + 1):
                        toCheck = list(map(set, combinations(s1, j + 1)))
                        if not all(
                            [s2 in frequentItemsets[j - 1] for s2 in toCheck]):
                            passes = False
                            break

                    if passes:
                        kItemsets += [s1]

                # Filter out non frequent and add result
                frequentItemsets += [
                    self._filterItemsets(
                        kItemsets, self._generateItemsetCounts(kItemsets))
                ]

        return frequentItemsets


class AssociationRules:
    def __init__(self, buckets, c, frequentItems):
        # Array of bucket containing items (ie: dataset)
        self.buckets = buckets
        # Array of sets. Index i => i-1 set of frequent items with support >s.
        # Confidence
        self.c = c

        # Key = frozenset
        # Value nb of support
        self.supportValue = dict()

        self.associationRules = set()

        associationRules = []
        for i in range(1, len(frequentItems)):
            # Don't compute single item
            tmpAR = []
            for v in frequentItems[i]:
                # Tuple of sets
                tmpAR.append((v, set()))
            associationRules.append(tmpAR)

        for ar in associationRules:
            self._generateAssociationRules(ar)

    def _generateAssociationRules(self, arrayPossibleAssociationRules):
        # From valid array of associationRules -> create new one by adding on element
        # Recursion
        if (len(arrayPossibleAssociationRules) == 0):
            # No association rules
            return

        self.supportValue = dict()

        possibleAssociationRules = []
        for fi in arrayPossibleAssociationRules:
            lPart = fi[0]
            rPart = fi[1]

            for elmt in lPart:

                nLPart = lPart.copy()
                nLPart = nLPart - {elmt}
                nLPart = frozenset(nLPart)

                nRPart = rPart.copy()
                nRPart = set(nRPart)
                nRPart.add(elmt)
                nRPart = frozenset(nRPart)

                if (len(nLPart) != 0 and len(nRPart) != 0):
                    self.supportValue[nLPart] = 0
                    self.supportValue[nRPart] = 0
                    possibleAssociationRules.append((nLPart, nRPart))

        self._computeSupport()
        # print(self.supportValue)
        validAssociation = []
        for pAR in possibleAssociationRules:
            if self._thresholdConfidence(pAR):
                validAssociation.append(pAR)

        for v in validAssociation:
            self.associationRules.add(v)

        self._generateAssociationRules(validAssociation)

    def _thresholdConfidence(self, tupleAssociation):
        confidence = self.supportValue[
            tupleAssociation[0]] / self.supportValue[tupleAssociation[1]]

        return (confidence > self.c)

    def _computeSupport(self):
        for b in self.buckets:
            for s in self.supportValue:
                if (s.intersection(b) == s):
                    self.supportValue[s] += 1

    def __repr__(self):
        value = ""
        for v in self.associationRules:
            value = value + "{} -> {}\n".format(set(v[0]), set(v[1]))
        value = value + "Confidence = {}\n".format(self.c)
        value = value + "Tot buckets = {}\n".format(len(self.buckets))
        value = value + "Nb association > c = {}\n".format(
            len(self.associationRules))

        return (value)
