/**
 * Computes the Hubs and Authorities for an every document in a query-specific
 * link graph, induced by the base set of pages.
 *
 * @author Dmytro Kalpakchi
 */

package ir;

import java.io.*;
import java.util.*;


public class HITSRanker {

    /**
     * Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     * Convergence criterion: hub and authority scores do not
     * change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<>();

    /**
     * Sparse vector containing hub scores
     */
    HashMap<Integer, Double> hubs = new HashMap<>();

    /**
     * Sparse vector containing authority scores
     */
    HashMap<Integer, Double> authorities = new HashMap<>();

    /**
     * Links from i to j
     */
    HashMap<Integer, HashSet<Integer>> link = new HashMap<>();

    /**
     * Links from j to i
     */
    HashMap<Integer, HashSet<Integer>> reverseLink = new HashMap<>();


    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * <p>
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * <p>
     * Each line in the links file has the following format:
     * nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * <p>
     * Each line in the titles file has the following format:
     * nodeID;pageTitle
     * <p>
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     * as docIDs used by search engine's Indexer
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages titles
     * @param index          The inverted index
     */
    public HITSRanker(String linksFilename, String titlesFilename, Index index) {
        this.index = index;
        readDocs(linksFilename, titlesFilename);
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param path The file path
     * @return The file name.
     */
    private String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages titles
     */
    void readDocs(String linksFilename, String titlesFilename) {
        try {
            File file = new File(titlesFilename);
            FileReader freader = new FileReader(file);
            try (BufferedReader br = new BufferedReader(freader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(";");
                    titleToId.put(data[1], Integer.parseInt(data[0]));
                }
            }
            freader.close();

            file = new File(linksFilename);
            freader = new FileReader(file);
            try (BufferedReader br = new BufferedReader(freader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    int index = line.indexOf(";");
                    Integer fromId = Integer.parseInt(line.substring(0, index));

                    StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                    while (tok.hasMoreTokens()) {
                        Integer otherId = Integer.parseInt(tok.nextToken());

                        link.computeIfAbsent(fromId, k -> new HashSet<>());
                        reverseLink.computeIfAbsent(otherId, k -> new HashSet<>());
                        if (!link.get(fromId).contains(otherId)) {
                            link.get(fromId).add(otherId);
                            reverseLink.get(otherId).add(fromId);
                        }
                    }
                }
            }
            freader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        // Clear hubs and authorities scores
        hubs.clear();
        authorities.clear();

        // Compute base set
        Set<Integer> baseSet = new HashSet<>();
        for (String title : titles) {
            Integer docID = titleToId.get(getFileName(title));

            // Should we ignore (?)
            if (docID == null) {
                continue;
            }

            // Add this root document
            baseSet.add(docID);

            // Add all documents linking from root document
            link.computeIfAbsent(docID, k -> new HashSet<>());
            baseSet.addAll(link.get(docID));

            // Add all documents linking to root document
            reverseLink.computeIfAbsent(docID, k -> new HashSet<>());
            baseSet.addAll(reverseLink.get(docID));
        }

        // Init scores
        for (Integer docID : baseSet) {
            authorities.put(docID, 1.0);
            hubs.put(docID, 1.0);
        }

        for (int i = 0; i < MAX_NUMBER_OF_STEPS; i++) {
            System.out.println("Iteration " + (i + 1));
            // Save old maps
            HashMap<Integer, Double> oldHubs = hubs;
            HashMap<Integer, Double> oldAuthorities = authorities;

            // Create new maps
            HashMap<Integer, Double> newHubs = new HashMap<>();
            HashMap<Integer, Double> newAuthorities = new HashMap<>();
            double sumHubs = 0.0;
            double sumAuthorities = 0.0;


            // Update hub and authority values for each document in base set
            for (Integer docID : baseSet) {

                // Calculate hub score
                double hubScore = 0.0;
                if (link.containsKey(docID)) {
                    for (Integer targetID : link.get(docID)) {
                        if (baseSet.contains(targetID)) {
                            hubScore += authorities.get(targetID);
                        }
                    }
                }
                newHubs.put(docID, hubScore);
                sumHubs += Math.pow(hubScore, 2);

                // Calculate authority score
                double authorityScore = 0.0;
                if (reverseLink.containsKey(docID)) {
                    for (Integer targetID : reverseLink.get(docID)) {
                        if (baseSet.contains(targetID)) {
                            authorityScore += hubs.get(targetID);
                        }
                    }
                }
                newAuthorities.put(docID, authorityScore);
                sumAuthorities += Math.pow(authorityScore, 2);
            }

            // Normalize
            sumHubs = Math.sqrt(sumHubs);
            sumAuthorities = Math.sqrt(sumAuthorities);
            for (Integer docID : baseSet) {
                newHubs.put(docID, newHubs.get(docID) / sumHubs);
                newAuthorities.put(docID, newAuthorities.get(docID) / sumAuthorities);
            }

            // Replace old maps with new
            hubs = newHubs;
            authorities = newAuthorities;

            // Stop if converged
            if (converged(oldHubs, oldAuthorities)) {
                break;
            }
        }
    }

    private boolean converged(HashMap<Integer, Double> oldHubs, HashMap<Integer, Double> oldAuthorities) {
        double hubsDiff = 0.0;
        double authoritiesDiff = 0.0;

        for (Integer docID : oldHubs.keySet()) {
            hubsDiff += Math.abs(oldHubs.get(docID) - hubs.get(docID));
            authoritiesDiff += Math.abs(oldAuthorities.get(docID) - authorities.get(docID));
        }

        return hubsDiff < EPSILON && authoritiesDiff < EPSILON;
    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param post The list of postings fulfilling a certain information need
     * @return A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        // Build rootSet
        Set<String> rootSet = new HashSet<>();
        for (PostingsEntry pe : post.list) {
            rootSet.add(index.docNames.get(pe.getDocID()));
        }

        // Compute scores
        iterate(rootSet.toArray(new String[0]));

        // Store ranks in PostingsEntries
        for (PostingsEntry pe : post.list) {
            String docName = index.docNames.get(pe.getDocID());
            docName = getFileName(docName);
            Integer docID = titleToId.get(docName);

            if (hubs.containsKey(docID) && authorities.containsKey(docID)) {
                pe.setScore(hubs.get(docID) + authorities.get(docID));
            } else {
                pe.setScore(0.0);
            }
        }

        return post;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     * @return A hash map sorted by values
     */
    private HashMap<Integer, Double> sortHashMapByValue(HashMap<Integer, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());

            Collections.sort(list, (o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

            HashMap<Integer, Double> res = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }
            return res;
        }
    }


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param map   A hash map
     * @param fname The filename
     * @param k     A number of entries to write
     */
    void writeToFile(HashMap<Integer, Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));

            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer, Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {
        }
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     * hubs_top_30.txt with documents containing top 30 hub scores
     * authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please give the names of the link and title files");
        } else {
            HITSRanker hr = new HITSRanker(args[0], args[1], null);
            hr.rank();
        }
    }
} 