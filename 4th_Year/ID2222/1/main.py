#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import time
from classes import *

def findSimilarDocuments(dataset, k=2, b=20, r=5, T=0.8):
    start_time = time.time()
    ssets = [Shingling(document, k, isFile=False) for document in dataset]
    minhash = MinHashing(k=b*r)
    mhshs = [minhash.minHash(sset.shingles) for sset in ssets]
    lsh = LSH(b, r, mhshs)

    similarDocuments = []
    for p in lsh.candidates:
        if CompareSets(ssets[p[0]].shingles, ssets[p[1]].shingles).similarity > T:
            similarDocuments += [p]
    stop_time = time.time()

    print("Possible pairs: " + str(((len(dataset)-1)*len(dataset))//2))
    print("Candidate pairs: " + str(len(lsh.candidates)))
    print("Similar documents: " + str(len(similarDocuments)))
    print()

    for sd in similarDocuments:
        if dataset[sd[0]] != dataset[sd[1]]:
            print()
            print("\"" + dataset[sd[0]] + "\" is similar to \"" + dataset[sd[1]] + "\"")
            print(CompareSets(ssets[sd[0]].shingles, ssets[sd[1]].shingles), end="")
            print(CompareSignatures(mhshs[sd[0]], mhshs[sd[1]]), end="")

    return stop_time - start_time

def debug():
    s1 = Shingling("testFiles/test1.txt", 4)
    s2 = Shingling("testFiles/test2.txt", 4)
    s3 = Shingling("testFiles/test3.txt", 4)
    s4 = Shingling("testFiles/test4.txt", 4)

    print(s1)
    print(s2)
    print(s3)
    print(s4)

    sim1 = CompareSets(s1.shingles, s2.shingles)
    sim2 = CompareSets(s1.shingles, s3.shingles)
    sim3 = CompareSets(s2.shingles, s3.shingles)
    sim4 = CompareSets(s3.shingles, s4.shingles)

    print(sim1)
    print(sim2)
    print(sim3)
    print(sim4)

    k = 100

    # b.r = k
    b = 20
    r = 5

    minhash = MinHashing(k=k)
    sig1 = minhash.minHash(s1.shingles)
    sig2 = minhash.minHash(s2.shingles)
    sig3 = minhash.minHash(s3.shingles)
    sig4 = minhash.minHash(s4.shingles)

    lsh = LSH(b, r, [sig1, sig2, sig3, sig4])
    print(lsh)

    esim1 = CompareSignatures(sig1, sig2)
    esim2 = CompareSignatures(sig1, sig3)
    esim3 = CompareSignatures(sig2, sig3)
    esim4 = CompareSignatures(sig3, sig4)

    print(esim1)
    print(esim2)
    print(esim3)
    print(esim4)

if __name__ == "__main__":
    dataset = pd.read_csv("testFiles/SMSSpamCollection", delimiter='\t', usecols=(1,), header=None)

    datasetSize = np.linspace(10, len(dataset), 50, dtype=int)
    times = []
    for i in datasetSize:
        times += [findSimilarDocuments(dataset.iloc[:, 0][:i])]


    plt.plot(datasetSize, times, '.b-')
    plt.xlabel('Size of dataset (number of documents)')
    plt.ylabel('Execution time (in seconds)')
    plt.savefig('datasetsize_execution.pdf')
    plt.clf()
