/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;
import java.util.stream.Collectors;


public class SpellChecker {
    /**
     * The regular inverted index to be used by the spell checker
     */
    Index index;

    /**
     * K-gram index to be used by the spell checker
     */
    KGramIndex kgIndex;

    /**
     * The auxiliary class for containing the value of your ranking function for a token
     */
    class KGramStat implements Comparable {
        double score;
        String token;
        int size;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
            this.size = 1;
        }

        KGramStat(String token, double score, int size) {
            this(token, score);
            this.size = size;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat) other).score) return 0;
            return this.score < ((KGramStat) other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
     * The threshold for edit distance for a candidate spelling
     * correction to be accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 2;

    private static final double JACCARD_WEIGHT = 1.0;
    private static final double EDIT_DISTANCE_WEIGHT = 1.0;
    private static final double FREQUENCY_WEIGHT = 10.0;

    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Computes the Jaccard coefficient for two sets A and B, where the size of set A is
     * <code>szA</code>, the size of set B is <code>szB</code> and the intersection
     * of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        return ((double) intersection) / ((double) szA + (double) szB - (double) intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     * => insert (cost 1)
     * => delete (cost 1)
     * => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        // Distance matrix
        int[][] d = new int[s1.length() + 1][s2.length() + 1];

        // Initialize first column
        for (int i = 0; i <= s1.length(); i++) {
            d[i][0] = i;
        }
        // Initialize first row
        for (int j = 0; j <= s2.length(); j++) {
            d[0][j] = j;
        }

        for (int j = 1; j <= s2.length(); j++) {
            int subCost;
            for (int i = 1; i <= s1.length(); i++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    subCost = 0;
                } else {
                    subCost = 2;
                }

                d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + subCost));
            }
        }

        return d[s1.length()][s2.length()];
    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        List<List<KGramStat>> candidates = new ArrayList<>();

        for (Query.QueryTerm queryTerm : query.queryterm) {
            String term = queryTerm.term;

            if (index.getPostings(term) == null) {
                // Generate k-grams
                String regex = "^" + term + "$";
                Set<String> kgrams = getKGrams(regex);

                // Get all words containing at least one of the k-grams and keep frequency (intersection set size)
                HashMap<String, Integer> candidates1 = new HashMap<>();
                for (String kgram : kgrams) {
                    List<KGramPostingsEntry> kgpes = kgIndex.getPostings(kgram);
                    for (KGramPostingsEntry kgpe : kgpes) {
                        String candidate = kgIndex.getTermByID(kgpe.tokenID);
                        candidates1.computeIfPresent(candidate, (k, v) -> v + 1);
                        candidates1.putIfAbsent(candidate, 1);
                    }
                }

                // Filter candidates with the Jaccard threshold
                List<KGramStat> candidates2 = new ArrayList<>();
                for (String candidate : candidates1.keySet()) {
                    // Keep candidate if jaccard similarity is higher than threshold
                    double jaccardSimilarity = jaccard(kgrams.size(), kgIndex.getFrequencyByTerm(candidate), candidates1.get(candidate));
                    if (jaccardSimilarity >= JACCARD_THRESHOLD) {
                        candidates2.add(new KGramStat(candidate, JACCARD_WEIGHT * jaccardSimilarity));
                    }
                }

                // Filter candidates with the edit distance threshold
                List<KGramStat> corrections = new ArrayList<>();
                for (KGramStat candidate : candidates2) {
                    int editDistance = editDistance(term, candidate.token);
                    if (editDistance <= MAX_EDIT_DISTANCE) {
                        double score = candidate.score +
                                1.0 / (EDIT_DISTANCE_WEIGHT * editDistance) +
                                FREQUENCY_WEIGHT * (((double) index.getPostings(candidate.token).size()) / ((double) index.docNames.size()));
                        corrections.add(new KGramStat(candidate.token, score));
                    }
                }

                candidates.add(corrections);
            } else {
                List<KGramStat> corrections = new ArrayList<>();
                corrections.add(new KGramStat(term, -1));
                candidates.add(corrections);
            }
        }

        // Call mergeCorrections
        List<KGramStat> corrections = mergeCorrections(candidates, limit);

        // Sort corrections
        corrections.sort(Comparator.comparingDouble(o -> -o.score));

        return corrections.stream().limit(limit).map(x -> x.token).toArray(String[]::new);
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available in
     * <code>qCorrections</code> into one final merging of query phrases. Returns up
     * to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        while (qCorrections.size() > 1) {
            List<KGramStat> tmp = mergeCorrectionsTwo(qCorrections.get(0), qCorrections.get(1), 2 * limit);
            qCorrections = qCorrections.subList(1, qCorrections.size());
            qCorrections.set(0, tmp);
        }
        return qCorrections.get(0);
    }

    private List<KGramStat> mergeCorrectionsTwo(List<KGramStat> left, List<KGramStat> right, int limit) {
        List<KGramStat> result = new ArrayList<>();

        for (KGramStat kGramStatl : left) {
            for (KGramStat kGramStatr : right) {
                String newTerm = kGramStatl.token + " " + kGramStatr.token;
                int size = mergeCorrectionsSize(kGramStatl, kGramStatr);
                double score = mergeCorrectionsScore(kGramStatl, kGramStatr, size);
                result.add(new KGramStat(newTerm, score, size));
            }
        }

        // Order candidates
        result.sort(Comparator.comparingDouble(o -> -o.score));

        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private int mergeCorrectionsSize(KGramStat left, KGramStat right) {
        return (left.score > 0 ? left.size : 0) + (right.score > 0 ? right.size : 0);
    }

    private double mergeCorrectionsScore(KGramStat left, KGramStat right, int size) {
        return ((double) (left.score > 0 ? left.size : 0) + (right.score > 0 ? right.size : 0)) / ((double) size);
    }

    private Set<String> getKGrams(String token) {
        Set<String> kgrams = new HashSet<>();
        int K = kgIndex.getK();
        for (int i = 0; i < token.length() - K + 1; i++) {
            kgrams.add(token.substring(i, i + K));
        }
        return kgrams;
    }

}
