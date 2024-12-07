from classes import *
import matplotlib.pyplot as plt
import numpy as np

def main():
    fd = runFD()
    print(fd)

def experiment():
    sizes = np.arange(500, 10000, 500)
    results = []
    for size in sizes:
        print(size)
        results += [[runFD(size).estimation() for i in range(20)]]
    means = [np.mean(result) for result in results]
    stds = [np.std(result) for result in results]
    plt.errorbar(sizes, means, yerr=stds, fmt='ob-', capsize=8, label='Estimation')
    plt.plot(sizes, [1612010 for i in sizes], 'g-', label='Effective')
    plt.xlabel('M (size of sample)')
    plt.ylabel('Number of triangles estimation')
    plt.legend(loc='lower right')
    plt.savefig('experiment.pdf')
    plt.clf()

def runFD(M = 5000):
    # This creates a generator so we don't end up OOM
    facebookCombinedStream = (["+", (int(row.split()[0]), int(row.split()[1]))] for row in open('./facebook_combined.txt'))
    fd = FullyDynamic(M)
    for spl in facebookCombinedStream:
        fd.sample(spl)
    return fd

if __name__ == "__main__":
    main()
    #experiment()
