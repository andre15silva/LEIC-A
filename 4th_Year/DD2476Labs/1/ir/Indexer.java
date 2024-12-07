/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /**
     * The index to be built up by this Indexer.
     */
    Index index;

    /**
     * K-gram index to be built up by this Indexer
     */
    KGramIndex kgIndex;

    /**
     * The next docID to be generated.
     */
    private int lastDocID = 0;

    /**
     * The patterns matching non-standard words (e-mail addresses, etc.)
     */
    String patterns_file;

    /* ----------------------------------------------- */


    /**
     * Constructor
     */
    public Indexer(Index index, KGramIndex kgIndex, String patterns_file) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }


    /**
     * Generates a new document identifier as an integer.
     */
    private int generateDocID() {
        return lastDocID++;
    }


    /**
     * Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     * all its files and subdirectories are recursively processed.
     */
    public void processFiles(File f, boolean is_indexing) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if (f.canRead()) {
                if (f.isDirectory()) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if (fs != null) {
                        for (int i = 0; i < fs.length; i++) {
                            processFiles(new File(f, fs[i]), is_indexing);
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if (docID % 1000 == 0) System.err.println("Indexed " + docID + " files");
                    try {
                        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                        Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);
                        int offset = 0;
                        while (tok.hasMoreTokens()) {
                            String token = tok.nextToken();
                            insertIntoIndex(docID, token, offset++);
                        }
                        index.docNames.put(docID, f.getPath());
                        index.docLengths.put(docID, offset);
                        reader.close();
                    } catch (IOException e) {
                        System.err.println("Warning: IOException during indexing.");
                    }
                }
            }
        }
    }

    public void computeEuclideanLengths() {
        if (new File("davisEuclideanLengths.txt").exists()) {
            // load from file if available
            try {
                File file = new File("davisEuclideanLengths.txt");
                FileReader freader = new FileReader(file);
                try (BufferedReader br = new BufferedReader(freader)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(";");
                        index.docEuclideanLengths.put(new Integer(data[0]), new Double(data[1]));
                    }
                }
                freader.close();
                System.out.println("Loaded " + index.docEuclideanLengths.size() + " euclidean lengths...");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // Compute lengths and store to file
            for (Integer docID : IntStream.range(0, lastDocID).boxed().collect(Collectors.toList())) {
                if (docID % 1000 == 0) System.err.println("Computed Euclidean Length for " + docID + " files");
                try {
                    File f = new File(index.docNames.get(docID));
                    Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                    Tokenizer tok = new Tokenizer(reader, true, false, true, patterns_file);
                    HashMap<String, Integer> tokenFrequency = new HashMap<>();
                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        if (tokenFrequency.containsKey(token)) {
                            tokenFrequency.put(token, tokenFrequency.get(token) + 1);
                        } else {
                            tokenFrequency.put(token, 1);
                        }
                    }

                    double euclideanLength = 0.0;
                    for (String token : tokenFrequency.keySet()) {
                        int tf = tokenFrequency.get(token);
                        int N = index.docNames.size();
                        int df_t = index.getPostings(token).size();
                        double idf = Math.log((double) N / (double) df_t);
                        euclideanLength += Math.pow(tf * idf, 2);
                    }
                    euclideanLength = Math.sqrt(euclideanLength);
                    index.docEuclideanLengths.put(docID, euclideanLength);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                FileOutputStream fout = new FileOutputStream("davisEuclideanLengths.txt");
                for (Map.Entry<Integer, Double> entry : index.docEuclideanLengths.entrySet()) {
                    Integer docID = entry.getKey();
                    String docInfoEntry = docID + ";" + entry.getValue() + "\n";
                    fout.write(docInfoEntry.getBytes());
                }
                fout.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printKGramIndexResults() {
        // First query : "ve"
        System.out.println("Query (k=2): \"ve\"");
        List<KGramPostingsEntry> postings = kgIndex.getPostings("ve");

        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }

        // First query : "ve"
        System.out.println("Query (k=2): \"th he\"");
        List<KGramPostingsEntry> postings1 = kgIndex.getPostings("th");
        List<KGramPostingsEntry> postings2 = kgIndex.getPostings("he");
        postings = kgIndex.intersect(postings1, postings2);

        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }

    }


    /* ----------------------------------------------- */


    /**
     * Indexes one token.
     */
    public void insertIntoIndex(int docID, String token, int offset) {
        index.insert(token, docID, offset);
        if (kgIndex != null)
            kgIndex.insert(token);
    }
}

