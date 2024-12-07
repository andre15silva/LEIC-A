/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;

    public ArrayList<Integer> offsets = new ArrayList<>();

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     * <p>
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    public PostingsEntry(int docId, int offset) {
        this.docID = docId;
        this.offsets.add(offset);
    }

    public int getDocID() {
        return docID;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public ArrayList<Integer> getOffsets() {
        return offsets;
    }

    public void addOffset(int offset) {
        offsets.add(offset);
    }

    public void addOrderedOffsets(List<Integer> incoming) {
        this.offsets.addAll(incoming);
        Collections.sort(this.offsets);
    }

    public static class PostingsEntryDocIDComparator implements Comparator<PostingsEntry> {
        public int compare(PostingsEntry p1, PostingsEntry p2) {
            return Integer.compare(p1.getDocID(), p2.getDocID());
        }
    }

}

