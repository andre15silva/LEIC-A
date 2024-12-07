package ir;

import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    /**
     * The intermediate dictionary files
     */
    private final Deque<Pair<RandomAccessFile, RandomAccessFile>> intermediateFiles = new ArrayDeque<>();

    private List<Thread> threads = new ArrayList<>();

    private boolean writtenToDocFile = false;

    private Boolean fileCreation = new Boolean(true);

    private int threshold = 6000;

    private String lastDictionaryFileName;

    private String lastDataFileFileName;

    public PersistentScalableHashedIndex() {
        try {
            synchronized (fileCreation) {
                dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
                dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
                lastDictionaryFileName = INDEXDIR + "/" + DICTIONARY_FNAME;
                lastDataFileFileName = INDEXDIR + "/" + DATA_FNAME;
            }
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

    @Override
    protected void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo", writtenToDocFile);
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
        writtenToDocFile = true;
    }

    @Override
    public void insert(String token, int docID, int offset) {
        super.insert(token, docID, offset);

        if (docID > threshold && docID != 0) {
            threshold += 6000;
            try {
                // Flush maps to files
                free = 0;
                writeDocInfo();
                writeIndex();

                synchronized (this.intermediateFiles) {
                    // Add current file to intermediate list
                    this.intermediateFiles.add(new Pair<>(dictionaryFile, dataFile));

                    if (this.intermediateFiles.size() >= 2) {
                        // Launch merge thread
                        Thread thread = new Thread(() -> mergeIntermediateFiles(this.intermediateFiles.pop(), this.intermediateFiles.pop()));
                        thread.start();
                        threads.add(thread);
                    }
                }

                // Create new additional file
                synchronized (fileCreation) {
                    dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + System.currentTimeMillis(), "rw");
                    dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + System.currentTimeMillis(), "rw");

                }

                // Clear variables
                this.index.clear();
                docNames.clear();
                docLengths.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void cleanup() {
        try {
            // Flush maps to files
            if (this.index.size() > 0) {
                free = 0;
                writeDocInfo();
                writeIndex();
            }

            // Clear variables
            this.index.clear();
            docNames.clear();
            docLengths.clear();

            int size;
            synchronized (this.intermediateFiles) {
                this.intermediateFiles.add(new Pair<>(dictionaryFile, dataFile));
                size = this.intermediateFiles.size();
            }

            while (!threads.isEmpty() || size != 0) {
                for (Thread thread : threads) {
                    thread.join();
                }
                threads.clear();
                synchronized (this.intermediateFiles) {
                    if (this.intermediateFiles.size() >= 2) {
                        Thread thread = new Thread(() -> mergeIntermediateFiles(this.intermediateFiles.pop(), this.intermediateFiles.pop()));
                        thread.start();
                        threads.add(thread);
                    } else if (this.intermediateFiles.size() == 1) {
                        System.out.println("Finished merging.");
                        Pair<RandomAccessFile, RandomAccessFile> p = this.intermediateFiles.pop();
                        dictionaryFile = p.getKey();
                        dataFile = p.getValue();
                        copyToOriginalFiles();
                        dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
                        dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
                    }
                    size = this.intermediateFiles.size();
                }
            }
            readDocInfo();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }

    private void mergeIntermediateFiles(Pair<RandomAccessFile, RandomAccessFile> p1, Pair<RandomAccessFile, RandomAccessFile> p2) {
        try {
            RandomAccessFile mergedDictionary, mergedDataFile;
            synchronized (fileCreation) {
                lastDictionaryFileName = INDEXDIR + "/" + DICTIONARY_FNAME + "_merged_" + System.currentTimeMillis();
                lastDataFileFileName = INDEXDIR + "/" + DATA_FNAME + "_merged_" + System.currentTimeMillis();
                mergedDictionary = new RandomAccessFile(lastDictionaryFileName, "rw");
                mergedDataFile = new RandomAccessFile(lastDataFileFileName, "rw");
            }

            // Init dictionary file
            byte[] bytes = new byte[(int) TABLESIZE * dictionaryEntrySize()];
            mergedDictionary.setLength(TABLESIZE * dictionaryEntrySize());
            mergedDictionary.write(bytes);

            RandomAccessFile p1Dictionary = p1.getKey();
            RandomAccessFile p1DataFile = p1.getValue();
            RandomAccessFile p2Dictionary = p2.getKey();
            RandomAccessFile p2DataFile = p2.getValue();

            HashSet<Long> pointers = new HashSet<>();
            HashSet<String> tokens = new HashSet<>();

            long free = 0L;
            for (long ptr = 0; ptr < (TABLESIZE * dictionaryEntrySize()); ptr += dictionaryEntrySize()) {
                Entry entry = readEntry(p1Dictionary, ptr);
                if (entry.size != 0) {
                    PostingsList pl = PostingsList.fromString(readData(p1DataFile, entry.ptr, entry.size));

                    // If token in file 2, merge PostingsList
                    PostingsList pl2 = getPostings(p2Dictionary, p2DataFile, pl.token);
                    if (pl2 != null) {
                        pl = mergePostingsList(pl, pl2);
                    }

                    // Write merged (or single) PostingList to the new file
                    String postingsList = pl.toString();
                    int size;
                    while ((size = writeData(mergedDataFile, postingsList, free)) == -1) continue;
                    long newPtr = h(pl.token) * dictionaryEntrySize();
                    while (pointers.contains(newPtr)) {
                        newPtr += dictionaryEntrySize() % (TABLESIZE * dictionaryEntrySize());
                    }
                    writeEntry(mergedDictionary, new Entry(free, size), newPtr);
                    pointers.add(newPtr);
                    tokens.add(pl.token);

                    // Increase data file pointer
                    free += size;
                }
            }

            for (long ptr = 0; ptr < (TABLESIZE * dictionaryEntrySize()); ptr += dictionaryEntrySize()) {
                Entry entry = readEntry(p2Dictionary, ptr);
                if (entry.size != 0) {
                    PostingsList pl = PostingsList.fromString(readData(p2DataFile, entry.ptr, entry.size));

                    // If token is in tokens, it was already merged with file 1
                    if (tokens.contains(pl.token))
                        continue;

                    // Else, write postings list. No need to merge because file 1 didn't have
                    String postingsList = pl.toString();
                    int size;
                    while ((size = writeData(mergedDataFile, postingsList, free)) == -1) continue;
                    long newPtr = h(pl.token) * dictionaryEntrySize();
                    while (pointers.contains(newPtr)) {
                        newPtr += dictionaryEntrySize() % (TABLESIZE * dictionaryEntrySize());
                    }
                    writeEntry(mergedDictionary, new Entry(free, size), newPtr);
                    pointers.add(newPtr);
                    tokens.add(pl.token);

                    // Increase data file pointer
                    free += size;
                }
            }

            synchronized (this.intermediateFiles) {
                this.intermediateFiles.add(new Pair<>(mergedDictionary, mergedDataFile));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private PostingsList mergePostingsList(PostingsList p1, PostingsList p2) {
        HashMap<Integer, PostingsEntry> entries = new HashMap<>();

        for (PostingsEntry entry : p1.list) {
            entries.put(entry.getDocID(), entry);
        }

        for (PostingsEntry entry : p2.list) {
            if (entries.containsKey(entry.getDocID())) {
                entries.get(entry.getDocID()).addOrderedOffsets(entry.offsets);
            } else {
                entries.put(entry.getDocID(), entry);
            }
        }

        PostingsList result = new PostingsList(p1.token);
        for (Integer key : entries.keySet().stream().sorted().collect(Collectors.toList())) {
            result.insertPostingsEntry(entries.get(key));
        }

        return result;
    }

    private void copyToOriginalFiles() {
        try {
            Files.copy(Paths.get(lastDictionaryFileName), Paths.get(INDEXDIR + "/" + DICTIONARY_FNAME), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(lastDataFileFileName), Paths.get(INDEXDIR + "/" + DATA_FNAME), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}