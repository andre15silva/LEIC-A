/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /**
     * The index to be searched by this Searcher.
     */
    Index index;

    /**
     * The k-gram index to be searched by this Searcher
     */
    KGramIndex kgIndex;

    /**
     * PageRankID to PageRank score
     */
    HashMap<Integer, Double> pageRanks = new HashMap<>();

    /**
     * FileName to PageRankID
     */
    HashMap<String, Integer> pageTitles = new HashMap<>();

    static Double PAGE_RANK_WEIGHT = 100.0;
    static Double TF_IDF_WEIGHT = 1.0;

    /**
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Searches the index for postings matching the query.
     *
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normalizationType) {
        switch (queryType) {
            case PHRASE_QUERY:
                return phraseQuery(query);
            case RANKED_QUERY:
                return rankedQuery(query, rankingType, normalizationType);
            case INTERSECTION_QUERY:
                return intersectionQuery(query);
            default:
                return null;
        }
    }

    public PostingsList intersectionQuery(Query query) {
        if (query.queryterm.size() == 0) {
            return new PostingsList("");
        } else if (query.queryterm.size() == 1) {
            return getPostings(query.queryterm.get(0).term);
        } else {
            PostingsList p1 = getPostings(query.queryterm.get(0).term);
            for (int i = 1; i < query.size(); i++) {
                PostingsList p2 = getPostings(query.queryterm.get(i).term);
                p1 = intersectPostingsLists(p1, p2);
                if (p1 == null) {
                    return null;
                }
            }
            return p1;
        }

    }

    private PostingsList intersectPostingsLists(PostingsList p1, PostingsList p2) {
        if (p1 == null || p2 == null) {
            return null;
        }
        PostingsList answer = new PostingsList("");
        int i = 0, j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).getDocID() == p2.get(j).getDocID()) {
                answer.insertPostingsEntry(p1.get(i));
                i++;
                j++;
            } else if (p1.get(i).getDocID() < p2.get(j).getDocID()) {
                i++;
            } else {
                j++;
            }
        }
        return answer;
    }

    public PostingsList phraseQuery(Query query) {
        if (query.queryterm.size() == 0) {
            return new PostingsList("");
        } else if (query.queryterm.size() == 1) {
            return getPostings(query.queryterm.get(0).term);
        } else {
            PostingsList p1 = getPostings(query.queryterm.get(0).term);
            for (int i = 1; i < query.size(); i++) {
                PostingsList p2 = getPostings(query.queryterm.get(i).term);
                p1 = positionalIntersectPostingsLists(p1, p2);
            }
            return p1;
        }
    }

    private PostingsList positionalIntersectPostingsLists(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList("");

        ListIterator<PostingsEntry> it1 = p1.list.listIterator();
        ListIterator<PostingsEntry> it2 = p2.list.listIterator();
        PostingsEntry pe1 = it1.next();
        PostingsEntry pe2 = it2.next();

        while (true) {

            // Same document
            if (pe1.getDocID() == pe2.getDocID()) {
                ListIterator<Integer> it3 = pe1.offsets.listIterator();
                ListIterator<Integer> it4 = pe2.offsets.listIterator();
                Integer offset1 = it3.next();
                Integer offset2 = it4.next();

                // Find offsets that are next to each other (offset1 + 1 == offset2)
                while (true) {
                    if (offset1 + 1 == offset2) {
                        answer.insertPostingsEntry(new PostingsEntry(pe2.getDocID(), offset2));
                        if (it3.hasNext() && it4.hasNext()) {
                            offset1 = it3.next();
                            offset2 = it4.next();
                        } else {
                            break;
                        }
                    } else if (offset1 + 1 < offset2) {
                        if (it3.hasNext()) {
                            offset1 = it3.next();
                        } else {
                            break;
                        }
                    } else {
                        if (it4.hasNext()) {
                            offset2 = it4.next();
                        } else {
                            break;
                        }
                    }
                }

                if (it1.hasNext() && it2.hasNext()) {
                    pe1 = it1.next();
                    pe2 = it2.next();
                } else {
                    break;
                }
            } else if (pe1.getDocID() < pe2.getDocID()) {
                if (it1.hasNext()) {
                    pe1 = it1.next();
                } else {
                    break;
                }
            } else {
                if (it2.hasNext()) {
                    pe2 = it2.next();
                } else {
                    break;
                }
            }
        }
        return answer;
    }

    public PostingsList rankedQuery(Query query, RankingType rankingType, NormalizationType normalizationType) {
        if (query.queryterm.size() == 0) {
            return new PostingsList("");
        } else {
            switch (rankingType) {
                case TF_IDF:
                    return rankedTFIDF(query, normalizationType);
                case COMBINATION:
                    return rankedCombination(query, normalizationType);
                case PAGERANK:
                    return rankedPageRank(query);
                case HITS:
                    return rankedHITS(query);
                default:
                    return null;
            }
        }
    }

    private PostingsList rankedHITS(Query query) {
        PostingsList result = new PostingsList("");

        Query expandedQuery = getExpandedQuery(query);

        // Iterate over terms in the query
        for (Query.QueryTerm queryTerm : expandedQuery.queryterm) {
            PostingsList postingsList = getPostings(queryTerm.term);

            // Add postings entry to result postings list and root set
            for (PostingsEntry postingsEntry : postingsList.list) {
                // Add PostingsEntry or merge to the result
                PostingsEntry existing = result.searchPostingsEntry(postingsEntry.getDocID());
                if (existing == null) {
                    result.insertPostingsEntry(postingsEntry);
                }
            }
        }

        System.out.println("Creating HITSRanker");
        HITSRanker hitsRanker = new HITSRanker("./pagerank/linksDavis.txt", "./pagerank/davisTitles.txt", index);
        System.out.println("Rank...");
        result = hitsRanker.rank(result);

        // Sort the PostingsList and return
        result.sortByScore();
        return result;
    }


    private void loadPageRanks() {
        if (pageRanks.isEmpty()) {
            try {
                BufferedReader pageRanks = new BufferedReader(new FileReader("./pagerank/davisPageRank.txt"));
                BufferedReader davisTitles = new BufferedReader(new FileReader("./pagerank/davisTitles.txt"));

                String entry;
                while ((entry = pageRanks.readLine()) != null) {
                    String[] entryA = entry.split(";");
                    this.pageRanks.put(Integer.parseInt(entryA[0]), Double.valueOf(entryA[1]));
                }
                while ((entry = davisTitles.readLine()) != null) {
                    String[] entryA = entry.split(";");
                    this.pageTitles.put(entryA[1], Integer.parseInt(entryA[0]));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private PostingsList rankedPageRank(Query query) {
        loadPageRanks();

        PostingsList result = new PostingsList("");

        Query expandedQuery = getExpandedQuery(query);

        // Iterate over terms in the query
        for (Query.QueryTerm queryTerm : expandedQuery.queryterm) {
            PostingsList postingsList = getPostings(queryTerm.term);

            // Get PageRank for each PostingsEntry
            for (PostingsEntry postingsEntry : postingsList.list) {
                String docName = index.docNames.get(postingsEntry.getDocID());

                double pageRank = 0.0;
                if (pageTitles.containsKey(docName)) {
                    pageRank = pageRanks.get(pageTitles.get(docName));
                }

                postingsEntry.setScore(pageRank);

                // Add PostingsEntry or merge to the result
                PostingsEntry existing = result.searchPostingsEntry(postingsEntry.getDocID());
                if (existing == null) {
                    result.insertPostingsEntry(postingsEntry);
                }
            }

        }

        // Sort the PostingsList and return
        result.sortByScore();
        return result;
    }

    private PostingsList rankedCombination(Query query, NormalizationType normalizationType) {
        loadPageRanks();

        PostingsList result = new PostingsList("");

        Query expandedQuery = getExpandedQuery(query);

        // Iterate over terms in the query
        for (Query.QueryTerm queryTerm : expandedQuery.queryterm) {
            PostingsList postingsList = getPostings(queryTerm.term);

            // Compute normalized tf_idf for each PostingsEntry
            for (PostingsEntry postingsEntry : postingsList.list) {
                double score = 0.0;
                int tf = postingsEntry.getOffsets().size();
                int N = index.docNames.size();
                int df_t = postingsList.size();
                double len_d;

                if (normalizationType == NormalizationType.NUMBER_OF_WORDS) {
                    len_d = index.docLengths.get(postingsEntry.getDocID());
                } else {
                    len_d = index.docEuclideanLengths.get(postingsEntry.getDocID());
                }

                double idf = Math.log((double) N / (double) df_t);
                double tf_idf = (tf * idf) / len_d;
                score += TF_IDF_WEIGHT * tf_idf;

                String docName = index.docNames.get(postingsEntry.getDocID());
                docName = docName.substring(docName.lastIndexOf("/") + 1);
                double pageRank = 0.0;
                if (pageTitles.containsKey(docName)) {
                    pageRank = pageRanks.get(pageTitles.get(docName));
                }
                score += PAGE_RANK_WEIGHT * pageRank;

                postingsEntry.setScore(score);
                // Add PostingsEntry or merge to the result
                PostingsEntry existing = result.searchPostingsEntry(postingsEntry.getDocID());
                if (existing == null) {
                    result.insertPostingsEntry(postingsEntry);
                } else {
                    existing.score += (1 - PAGE_RANK_WEIGHT) * tf_idf;
                }
            }

        }

        // Sort the PostingsList and return
        result.sortByScore();
        return result;
    }

    private PostingsList rankedTFIDF(Query query, NormalizationType normalizationType) {
        PostingsList result = new PostingsList("");

        Query expandedQuery = getExpandedQuery(query);

        // Iterate over terms in the query
        for (Query.QueryTerm queryTerm : expandedQuery.queryterm) {
            PostingsList postingsList = getPostings(queryTerm.term);

            // Compute normalized tf_idf for each PostingsEntry
            for (PostingsEntry postingsEntry : postingsList.list) {
                int tf = postingsEntry.getOffsets().size();
                int N = index.docNames.size();
                int df_t = postingsList.size();
                double len_d;

                if (normalizationType == NormalizationType.NUMBER_OF_WORDS) {
                    len_d = index.docLengths.get(postingsEntry.getDocID());
                } else {
                    len_d = index.docEuclideanLengths.get(postingsEntry.getDocID());
                }

                double idf = Math.log((double) N / (double) df_t);
                double tf_idf = queryTerm.weight * ((tf * idf) / len_d);
                postingsEntry.setScore(tf_idf);

                // Add PostingsEntry or merge to the result
                PostingsEntry existing = result.searchPostingsEntry(postingsEntry.getDocID());
                if (existing == null) {
                    result.insertPostingsEntry(postingsEntry);
                } else {
                    existing.score += postingsEntry.getScore();

                }
            }

        }

        // Sort the PostingsList and return
        result.sortByScore();

        return result;
    }

    private List<String> getMatchedTokens(String term) {
        int position = term.indexOf("*");

        // Generate regex and k-grams (no duplicates)
        String regex;
        Set<String> kgrams = new HashSet<>();
        int K = kgIndex.getK();
        if (position == 0) {
            regex = term + "$";
            String token = regex.replace("*", "");
            for (int i = 0; i < token.length() - K + 1; i++) {
                kgrams.add(token.substring(i, i + K));
            }
        } else if (position == (term.length() - 1)) {
            regex = "^" + term;
            String token = regex.replace("*", "");
            for (int i = 0; i < token.length() - K + 1; i++) {
                kgrams.add(token.substring(i, i + K));
            }
        } else {
            regex = "^" + term + "$";
            String token = regex.split("\\*")[0];
            for (int i = 0; i < token.length() - K + 1; i++) {
                kgrams.add(token.substring(i, i + K));
            }
            token = regex.split("\\*")[1];
            for (int i = 0; i < token.length() - K + 1; i++) {
                kgrams.add(token.substring(i, i + K));
            }
        }

        // Intersection search on k-gram index
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != K) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + K + "-gram");
                return null;
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }

        // Post-process the results using the regex library
        List<String> result = new ArrayList<>();
        regex = regex.replace("*", ".*");
        Pattern p = Pattern.compile(regex);
        for (KGramPostingsEntry kgpe : postings) {
            Matcher m = p.matcher(kgIndex.getTermByID(kgpe.tokenID));
            if (m.matches()) {
                result.add(kgIndex.getTermByID(kgpe.tokenID));
            }
        }

        return result;
    }

    private PostingsList unionSearch(List<String> tokens) {
        PostingsList result = new PostingsList("");

        // Iterate over terms in the query
        for (String token : tokens) {
            PostingsList postingsList = getPostings(token);

            for (PostingsEntry postingsEntry : postingsList.list) {
                // Add PostingsEntry or merge to the result
                PostingsEntry existing = result.searchPostingsEntry(postingsEntry.getDocID());
                if (existing == null) {
                    result.insertPostingsEntry(postingsEntry);
                } else {
                    existing.addOrderedOffsets(postingsEntry.offsets);
                }
            }
        }

        result.sortByDocID();
        return result;
    }

    private PostingsList getPostings(String token) {
        if (token.contains("*")) {
            return unionSearch(getMatchedTokens(token));
        } else {
            return index.getPostings(token);
        }
    }

    private Query getExpandedQuery(Query query) {
        Query result = new Query();

        for (Query.QueryTerm queryTerm : query.queryterm) {
            if (queryTerm.term.contains("*")) {
                List<String> tokens = getMatchedTokens(queryTerm.term);
                for (String token : tokens) {
                    result.addQueryTerm(token, queryTerm.weight);
                }
            } else {
                result.addQueryTerm(queryTerm.term, queryTerm.weight);
            }
        }

        return result;
    }

}