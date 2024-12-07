# -*- coding: utf-8 -*-

import networkx as nx
from networkx.generators.community import LFR_benchmark_graph
from networkx.algorithms.core import k_shell
import random
from SIS import SIS
from SIR import SIR
from BFS import BFS
from Experiment import Experiment
from RandomStrategy import RandomStrategy
from KCoreStrategy import KCoreStrategy
from KShellStrategy import KShellStrategy
from NodeDegreeStrategy import NodeDegreeStrategy
from BetweenessCentralityStrategy import BetweenessCentralityStrategy
from EigenvectorCentralityStrategy import EigenvectorCentralityStrategy
from HybridCentralityStrategy import HybridCentralityStrategy
from MixedDegreeDecompositionStrategy import MixedDegreeDecompositionStrategy
import matplotlib.pyplot as plt
import numpy as np

def plot_results(results, title):
    betas=np.linspace(0, 1, num=11)
    #removals=np.linspace(0, 1, num=9)[:-1]
    removals=np.linspace(0, 0.05, num=6)[:-1]

    plt.clf()
    for i, take in enumerate(results):
        x = np.array(betas)
        y = np.array(take[0])
        std = np.array(take[1])
        ci = 1.96 * (std / y) # 95%
        plt.errorbar(x, y, std, fmt=".-", label="{:.1f}%".format(removals[i]*100), capsize=2.5)
#        plt.plot(x, y, ".-", label="{:.1f}%".format(removals[i]*100))
#        plt.fill_between(x, (y-std), (y+std), color="blue", alpha=0.1)

    plt.title(title)
    plt.xlabel("beta (β)")
    plt.ylabel("Proportion of infected nodes")
#    plt.yscale("log")
    plt.grid(linestyle="--", alpha=0.5)
    plt.legend(bbox_to_anchor=(1.04,1), borderaxespad=0).set_title("Removed nodes (ε)")
    plt.savefig(f"{title}.png", bbox_inches="tight")


def add_strategy_to_plot(i, results, title):
    #removals=np.linspace(0, 1, num=9)[:-1]
    removals=np.linspace(0, 0.05, num=6)[:-1]

    y = []
    stds = []
    for j, take in enumerate(results):
        y += [take[0][i]]
        stds += [take[1][i]]
    y = np.array(y)
    stds = np.array(stds)
    ci = 1.96 * (stds / y) # 95%
    plt.errorbar(removals, y, stds, fmt=".-", label=title, capsize=2.5)
#    plt.plot(removals, y, ".-", label=title)
#    plt.fill_between(removals, (y-stds), (y+stds), color="blue", alpha=0.1)


def plot_results_per_beta(strategies, title):
    betas=np.linspace(0, 1, num=11)

    for i, beta in enumerate(betas):
        plt.clf()
        for strategy in strategies: 
            add_strategy_to_plot(i, strategy[0], strategy[1])

        plt.title("{} β={:.1f}".format(title, beta))
        plt.xlabel("Proportion of removed nodes (ε)")
        plt.ylabel("Proportion of infected nodes")
        plt.grid(linestyle="--", alpha=0.5)
        plt.legend(bbox_to_anchor=(1.04,1), borderaxespad=0).set_title("Strategy")
        plt.savefig(f"{title}-{beta}.png", bbox_inches="tight")


def run_bfs_experiments(G):
    strategies = []
    #removals=np.linspace(0, 1, num=9)[:-1]
    removals=np.linspace(0, 0.05, num=6)[:-1]

    # BFS x KCore
    e1 = Experiment(G, KCoreStrategy, BFS, iterations=10, removals=removals)
    kcore_results = e1.run()
    plot_results(kcore_results, "BFS x KCore")
    strategies += [(kcore_results, "KCore")]

    # BFS x KShell
    e2 = Experiment(G, KShellStrategy, BFS, iterations=10, removals=removals)
    kshell_results = e2.run()
    plot_results(kshell_results, "BFS x KShell")
    strategies += [(kshell_results, "KShell")]

    # BFS x Random
    e3 = Experiment(G, RandomStrategy, BFS, iterations=10, removals=removals)
    random_results = e3.run()
    plot_results(random_results, "BFS x Random")
    strategies += [(random_results, "Random")]

    # BFS x DegreeCentrality
    e4 = Experiment(G, NodeDegreeStrategy, BFS, iterations=10, removals=removals)
    nodedegree_results = e4.run()
    plot_results(nodedegree_results, "BFS x DegreeCentrality")
    strategies += [(nodedegree_results, "DegreeCentrality")]

    # BFS x BetweenessCentrality
    e5 = Experiment(G, BetweenessCentralityStrategy, BFS, iterations=10, removals=removals)
    bc_results = e5.run()
    plot_results(bc_results, "BFS x BetweenessCentrality")
    strategies += [(bc_results, "BetweenessCentrality")]

    # BFS x EigenvectorCentrality
    e6 = Experiment(G, EigenvectorCentralityStrategy, BFS, iterations=10, removals=removals)
    ec_results = e6.run()
    plot_results(ec_results, "BFS x EigenvectorCentrality")
    strategies += [(ec_results, "EigenvectorCentrality")]

    # BFS x HybridCentrality
    e7 = Experiment(G, HybridCentralityStrategy, BFS, iterations=10, removals=removals)
    hc_results = e7.run()
    plot_results(hc_results, "BFS x HybridCentrality")
    strategies += [(hc_results, "HybridCentrality")]

    # BFS x MDD
    e8 = Experiment(G, MixedDegreeDecompositionStrategy, BFS, iterations=10, removals=removals)
    mdd_results = e8.run()
    plot_results(mdd_results, "BFS x MDD")
    strategies += [(mdd_results, "MDD")]

    plot_results_per_beta(strategies, "BFS")


def run_sis_experiments(G):
    strategies = []
    #removals=np.linspace(0, 1, num=9)[:-1]
    removals=np.linspace(0, 0.05, num=6)[:-1]

    # SIS x KCore
    e1 = Experiment(G, KCoreStrategy, SIS, iterations=10, removals=removals)
    kcore_results = e1.run()
    plot_results(kcore_results, "SIS x KCore")
    strategies += [(kcore_results, "KCore")]

    # SIS x KShell
    e2 = Experiment(G, KShellStrategy, SIS, iterations=10, removals=removals)
    kshell_results = e2.run()
    plot_results(kshell_results, "SIS x KShell")
    strategies += [(kshell_results, "KShell")]

    # SIS x Random
    e3 = Experiment(G, RandomStrategy, SIS, iterations=10, removals=removals)
    random_results = e3.run()
    plot_results(random_results, "SIS x Random")
    strategies += [(random_results, "Random")]

    # SIS x DegreeCentrality
    e4 = Experiment(G, NodeDegreeStrategy, SIS, iterations=10, removals=removals)
    nodedegree_results = e4.run()
    plot_results(nodedegree_results, "SIS x DegreeCentrality")
    strategies += [(nodedegree_results, "DegreeCentrality")]

    # SIS x BetweenessCentrality
    e5 = Experiment(G, BetweenessCentralityStrategy, SIS, iterations=10, removals=removals)
    bc_results = e5.run()
    plot_results(bc_results, "SIS x BetweenessCentrality")
    strategies += [(bc_results, "BetweenessCentrality")]

    # SIS x EigenvectorCentrality
    e6 = Experiment(G, EigenvectorCentralityStrategy, SIS, iterations=10, removals=removals)
    ec_results = e6.run()
    plot_results(ec_results, "SIS x EigenvectorCentrality")
    strategies += [(ec_results, "EigenvectorCentrality")]

    # SIS x HybridCentrality
    e7 = Experiment(G, HybridCentralityStrategy, SIS, iterations=10, removals=removals)
    hc_results = e7.run()
    plot_results(hc_results, "SIS x HybridCentrality")
    strategies += [(hc_results, "HybridCentrality")]

    # BFS x MDD
    e8 = Experiment(G, MixedDegreeDecompositionStrategy, SIS, iterations=10, removals=removals)
    mdd_results = e8.run()
    plot_results(mdd_results, "SIS x MDD")
    strategies += [(mdd_results, "MDD")]

    plot_results_per_beta(strategies, "SIS")


def main():
    # Create an LFR graph with no self-loops
    nodes = 10**5
    tau1 = 3
    tau2 = 1.5
    mu = 0.1
    G = LFR_benchmark_graph(nodes,
                            tau1,
                            tau2,
                            mu,
                            average_degree=13,
                            min_community=20,
                            seed=10)
    G.remove_edges_from(nx.selfloop_edges(G))

    # Run BFS experiments on G
    #run_bfs_experiments(G)

    # Run SIS experiments on G
    run_sis_experiments(G)

if __name__ == "__main__":
    main()
