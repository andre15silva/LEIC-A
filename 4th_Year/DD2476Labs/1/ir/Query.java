/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.stream.IntStream;


/**
 * A class for representing a query as a list of words, each of which has
 * an associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    class QueryTerm {
        String term;
        double weight;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }
    }

    /**
     * Representation of the query as a list of terms with associated weights.
     * In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms).
     * Should be between 0 and 1.
     * (only used in assignment 3).
     */
    double alpha = 0.2;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user).
     * (only used in assignment 3).
     */
    double beta = 1 - alpha;


    /**
     * Creates a new empty Query
     */
    public Query() {
    }


    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), 1.0));
        }
    }


    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }


    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }


    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }


    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     * @param engine        The search engine object
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {
        // Apply alpha
        for (QueryTerm queryTerm : queryterm) {
            queryTerm.weight *= alpha;
        }

        // Add mean of relevant docs vectors
        long nRelevantDocs = IntStream.range(0, docIsRelevant.length).mapToObj(i -> docIsRelevant[i]).filter(x -> x).count();
        for (int i = 0; i < docIsRelevant.length && i < results.list.size(); i++) {
            PostingsEntry postingsEntry = results.list.get(i);
            int docID = postingsEntry.getDocID();
            if (docIsRelevant[i]) {
                // Go over the entire document

                try {
                    File f = new File(engine.index.docNames.get(docID));
                    Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                    Tokenizer tok = new Tokenizer(reader, true, false, true, engine.patterns_file);
                    HashMap<String, Integer> tokenFrequency = new HashMap<>();
                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        if (tokenFrequency.containsKey(token)) {
                            tokenFrequency.put(token, tokenFrequency.get(token) + 1);
                        } else {
                            tokenFrequency.put(token, 1);
                        }
                    }

                    for (String token : tokenFrequency.keySet()) {
                        // Check if query already has this term
                        boolean found = false;
                        for (QueryTerm queryTerm : queryterm) {
                            if (queryTerm.term.equals(token)) {
                                found = true;
                                queryTerm.weight += beta * tokenFrequency.get(token) / nRelevantDocs;
                            }
                        }

                        if (!found) {
                            queryterm.add(new QueryTerm(token, beta * tokenFrequency.get(token) / nRelevantDocs));
                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void addQueryTerm(String term, Double weight) {
        queryterm.add(new QueryTerm(term, weight));
    }
}


