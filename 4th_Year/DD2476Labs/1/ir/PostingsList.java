/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PostingsList {

    /**
     * The postings list
     */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /**
     * O(1) access to PostingsEntry, to check if the PostingEntry is here
     */
    private HashMap<Integer, PostingsEntry> map = new HashMap<>();

    /**
     * Token it represents
     */
    public String token;

    /**
     * Number of postings in this list.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the ith posting.
     */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public PostingsList(String token) {
        this.token = token;
    }

    public void insertPostingsEntry(PostingsEntry postingsEntry) {
        if (!map.containsKey(postingsEntry.getDocID())) {
            list.add(postingsEntry);
            map.put(postingsEntry.getDocID(), postingsEntry);
        } else {
            map.get(postingsEntry.getDocID()).addOrderedOffsets(Collections.singletonList(postingsEntry.getOffsets().get(0)));
        }
    }

    public PostingsEntry searchPostingsEntry(int docID) {
        return map.get(docID);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(token.length());
        output.append(';');
        output.append(token);

        for (PostingsEntry pe : list) {
            output.append(pe.docID);
            for (Integer offset : pe.offsets) {
                output.append(',');
                output.append(offset);
            }
            output.append(';');
        }

        return output.toString();
    }

    public static PostingsList fromString(String data) {
        int splitLocation = data.indexOf(';');
        int size = Integer.parseInt(data.substring(0, splitLocation));

        String token = data.substring(splitLocation + 1, splitLocation + size + 1);
        PostingsList postingsList = new PostingsList(token);

        String list = data.substring(splitLocation + size + 1);
        String[] postingEntries = list.split(";");
        for (String pe : postingEntries) {
            String[] offsets = pe.split(",");
            PostingsEntry postingsEntry = new PostingsEntry(Integer.parseInt(offsets[0]), Integer.parseInt(offsets[1]));
            if (offsets.length > 2) {
                for (int i = 2; i < offsets.length; i++) {
                    postingsEntry.addOffset(Integer.parseInt(offsets[i]));
                }
            }
            postingsList.insertPostingsEntry(postingsEntry);
        }

        return postingsList;
    }

    public void sortByScore() {
        Collections.sort(this.list);
    }

    public void sortByDocID() {
        Collections.sort(this.list, new PostingsEntry.PostingsEntryDocIDComparator());
    }
}

