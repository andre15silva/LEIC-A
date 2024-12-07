#!/usr/bin/env python3
import random
import sympy
import zlib
import numpy as np


class Shingling:
    def __init__(self, document, k, isFile=True):
        self.document = document
        self.k = k
        if isFile:
            self._setContent()
        else:
            self.content = document
        self._setShingles()

    def _setContent(self):
        f = open(self.document, "r")
        # Read all the doc and replace final \n
        self.content = f.read().replace("\n", "")
        f.close()

    def _setShingles(self):
        self.shingles = sorted(
            set([
                self._hash(shingle) for shingle in [
                    self.content[i:i + self.k]
                    for i in range(len(self.content) - self.k + 1)
                ]
            ]))

    def _hash(self, valueToHash):
        # CRC32 hash
        # https://docs.python.org/3/library/zlib.html
        # Binary AND to be compatible with all python version
        value = zlib.crc32(valueToHash.encode()) & 0xffffffff
        return (value)

    def __repr__(self):
        value = "Document: {}\n".format(self.document)
        value = value + "K = {}\n".format(self.k)
        value = value + "nbShingles = {}\n".format(len(self.shingles))

        return (value)


class CompareSets:
    def __init__(self, set1, set2):
        self.set1 = set1
        self.set2 = set2
        self.similarity = self._jaccardSimilarity()

    def _jaccardSimilarity(self):
        nbSim = 0

        for s1 in self.set1:
            if s1 in self.set2:
                nbSim += 1

        return (nbSim / (len(self.set1) + len(self.set2) - nbSim))

    def __repr__(self):
        value = "Set1 : {}\n".format(len(self.set1))
        value = value + "Set2 : {}\n".format(len(self.set2))

        value = value + "Similarity {:.4f} = {:.2f}%\n".format(
            self.similarity, self.similarity * 100)

        return (value)


class MinHashing:
    def __init__(self, maxVal=2**32, k=100):
        # k hash-functions h(x)=(ax+b)%c
        # a and b are random integeres less than max val of x
        # c is a prime number slightly larger than max val of x
        self.k = k
        self.a = random.sample(range(1, maxVal), k)
        self.b = random.sample(range(1, maxVal), k)
        self.c = sympy.nextprime(maxVal + 1)

    def _hash(self, i, value):
        return (self.a[i] * value + self.b[i]) % self.c

    def minHash(self, set1):
        return [
            min({j: self._hash(i, j)
                 for j in set1}.items(),
                key=lambda x: x[1])[0] for i in range(self.k)
        ]


class CompareSignatures:
    def __init__(self, sig1, sig2):
        self.sig1 = sig1
        self.sig2 = sig2
        self.similarity = self._signatureSimilarity()

    def _signatureSimilarity(self):
        return sum([
            True if self.sig1[i] == self.sig2[i] else False
            for i in range(len(self.sig1))
        ]) / len(self.sig1)

    def __repr__(self): return "Estimated Similarity {:.4f} = {:.2f}%\n".format(
            self.similarity, self.similarity * 100)


class LSH:
    def __init__(self, b, r, listMinHash):
        # b number of bands
        # r number of rows
        # listMinHash array of sets of minHash to compute

        self.b = b
        self.r = r

        self.minHashes = listMinHash

        self._generateHashBands()
        self._generateBuckets()

    def _hash(self, value):
        return (hash(value))

    def _generateHashBands(self):
        self.bands = []
        for b in range(self.b):
            tmpBand = []
            for mh in self.minHashes:
                # Need to be tuple otherwise can't use built in hash function
                signature = tuple(mh[b * self.r:(b + 1) * self.r])
                tmpBand.append(self._hash(signature))
            self.bands.append(tmpBand)

    def _generateBuckets(self):
        self.candidates = set()
        for b in self.bands:
            pairs = self._generatePairs(b)
            for p in pairs:
                self.candidates.add(p)

    def _generatePairs(self, listHash):
        npArray = np.array(listHash)
        pairs = []

        for i in range(len(npArray)):
            # Get all index with similar hash value
            res = np.where(npArray[i + 1:] == npArray[i])[0]
            tmpPairs = []
            for r in res:
                tmpPairs.append((i, r + i + 1))
            pairs += tmpPairs

        # Sort so we can filter out (a,b) and (b,a) candidates
        return (pairs)

    def __repr__(self):
        value = "LSH\n"
        value = value + "b = {}\n".format(self.b)
        value = value + "r = {}\n".format(self.r)
        value = value + "nbPairs = {}\n".format(len(self.candidates))
        value = value + "pairs = {}\n".format(self.candidates)

        return (value)
