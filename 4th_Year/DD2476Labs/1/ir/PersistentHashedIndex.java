/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/*
 *   Implements an inverted index as a hashtable on disk.
 *
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks.
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /**
     * The directory where the persistent index files are stored.
     */
    public static final String INDEXDIR = "./index";

    /**
     * The dictionary file name
     */
    public static final String DICTIONARY_FNAME = "dictionary";

    /**
     * The dictionary file name
     */
    public static final String DATA_FNAME = "data";

    /**
     * The terms file name
     */
    public static final String TERMS_FNAME = "terms";

    /**
     * The doc info file name
     */
    public static final String DOCINFO_FNAME = "docInfo";

    /**
     * The dictionary hash table on disk can fit this many entries.
     */
    public static final long TABLESIZE = 611953L;
    //public static final long TABLESIZE = 3500000L;

    /**
     * The dictionary hash table is stored in this file.
     */
    RandomAccessFile dictionaryFile;

    /**
     * The data (the PostingsLists) are stored in this file.
     */
    RandomAccessFile dataFile;

    /**
     * Pointer to the first free memory cell in the data file.
     */
    long free = 0L;

    /**
     * The cache as a main-memory hash map.
     */
    HashMap<String, PostingsList> index = new HashMap<>();

    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        long ptr;
        int size;

        public Entry(long ptr, int size) {
            this.ptr = ptr;
            this.size = size;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "ptr=" + ptr +
                    ", size=" + size +
                    '}';
        }
    }


    // ==================================================================


    /**
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(RandomAccessFile dataFile, String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Reads data from the data file
     */
    String readData(RandomAccessFile dataFile, long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(RandomAccessFile dictionaryFile, Entry entry, long ptr) {
        try {
            dictionaryFile.seek(ptr);
            dictionaryFile.writeLong(entry.ptr);
            dictionaryFile.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(RandomAccessFile dictionaryFile, long ptr) {
        try {
            dictionaryFile.seek(ptr);
            long ptrr = dictionaryFile.readLong();
            int size = dictionaryFile.readInt();
            return new Entry(ptrr, size);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    long h(String w) {
        return Math.abs(w.hashCode()) % TABLESIZE;
    }

    int dictionaryEntrySize() {
        return 8 + 4;
    }


    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    protected void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    protected void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }


    /**
     * Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        HashSet<Long> pointers = new HashSet<>();

        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Init dictionary file
            byte[] bytes = new byte[(int) TABLESIZE * dictionaryEntrySize()];
            dictionaryFile.setLength(TABLESIZE * dictionaryEntrySize());
            dictionaryFile.write(bytes);

            // Write the dictionary and the postings list
            for (String token : index.keySet()) {
                // Write postingsList to data file
                String postingsList = index.get(token).toString();
                int size;
                while ((size = writeData(dataFile, postingsList, free)) == -1) continue;

                // Write entry in dictionary
                long ptr = h(token) * dictionaryEntrySize();
                while (pointers.contains(ptr)) {
                    // Resolve hashing collision
                    collisions++;
                    ptr += dictionaryEntrySize() % (TABLESIZE * dictionaryEntrySize());
                }
                writeEntry(dictionaryFile, new Entry(free, size), ptr);
                pointers.add(ptr);

                // Increase data file pointer
                free += size;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions.");
    }

    // ==================================================================


    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        if (index.containsKey(token)) {
            return index.get(token);
        } else {
            PostingsList result = getPostings(dictionaryFile, dataFile, token);
            if (result != null)
                index.put(token, result);
            return result;
        }
    }

    public PostingsList getPostings(RandomAccessFile dictionaryFile, RandomAccessFile dataFile, String token) {
        long ptr = h(token) * dictionaryEntrySize();

        // Read entry
        Entry entry = readEntry(dictionaryFile, ptr);

        if (entry.size == 0) {
            // token is not present
            return null;
        }

        // Read first hit
        String data = readData(dataFile, entry.ptr, entry.size);
        PostingsList postingsList = PostingsList.fromString(data);
        long og_ptr = ptr;
        ptr += dictionaryEntrySize() % (TABLESIZE * dictionaryEntrySize());

        while (!postingsList.token.equals(token) && ptr != og_ptr) {
            entry = readEntry(dictionaryFile, ptr);
            if (entry.size == 0) {
                // token is not present
                return null;
            }
            data = readData(dataFile, entry.ptr, entry.size);
            postingsList = PostingsList.fromString(data);
            ptr += dictionaryEntrySize() % (TABLESIZE * dictionaryEntrySize());
        }

        if (ptr == og_ptr) {
            // Scanned the whole index and didn't find anything
            return null;
        }

        return postingsList;
    }


    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        if (!this.index.containsKey(token)) {
            this.index.put(token, new PostingsList(token));
        }
        // score (?)
        this.index.get(token).insertPostingsEntry(new PostingsEntry(docID, offset));
    }


    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        writeIndex();
        System.err.println("done!");
    }
}
