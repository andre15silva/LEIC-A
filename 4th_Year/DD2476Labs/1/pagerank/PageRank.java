import javax.sound.midi.SysexMessage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we
     * don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * A memory-efficient representation of the transition matrix.
     * The outlinks are represented as a HashMap, whose keys are
     * the numbers of the documents linked from.<p>
     * <p>
     * The value corresponding to key i is a HashMap whose keys are
     * all the numbers of documents j that i links to.<p>
     * <p>
     * If there are no outlinks from i, then the value corresponding
     * key i is null.
     */
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<>();

    /**
     * The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     * The probability that the surfer will be bored, stop
     * following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * Convergence criterion: Transition probabilities do not
     * change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;


    /* --------------------------------------------- */


    public PageRank(String filename) {
        int noOfDocs = readDocs(filename);
        //iterate(noOfDocs, 1000);
        //experiment(noOfDocs, 1000, 0.85);
        algorithm4(noOfDocs, 25*noOfDocs, 0.85, true);
    }


    /* --------------------------------------------- */


    /**
     * Reads the documents and fills the data structures.
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                //  Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }


    /* --------------------------------------------- */

    private class PageRankResult implements Comparable<PageRankResult> {

        private double rank;

        private String docName;

        public PageRankResult(double rank, String docName) {
            this.rank = rank;
            this.docName = docName;
        }

        public double getRank() {
            return rank;
        }

        public int compareTo(PageRankResult other) {
            return Double.compare(other.getRank(), rank);
        }

        @Override
        public String toString() {
            return docName + ": " + String.format("%.5f", rank);
        }
    }

    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    double[] iterate(int numberOfDocs, int maxIterations) {
        // Initialize vectors
        double[] x;
        double[] x_ = new double[numberOfDocs];
        Arrays.fill(x_, 1.0 / numberOfDocs);

        for (int i = 0; i < maxIterations; i++) {
            x = x_;
            x_ = multiply(x, numberOfDocs);

            if (converged(x, x_)) {
                break;
            }
        }

        // Sort documents by PageRank
        double[] finalX_ = x_;
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(finalX_[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        // Print top 30
        for (int i = 0; i < 30; i++) {
            System.out.println(sortedPageRankResults.get(i));
        }

        // Write to file
        try {
            FileWriter fout = new FileWriter("davisPageRank.txt");
            for (int i = 0; i < numberOfDocs; i++) {
                PageRankResult result = sortedPageRankResults.get(i);
                String output = result.docName + ";" + String.format("%.5f", result.rank) + " \n";
                fout.write(output);
            }
            fout.close();
        } catch (IOException e) {
            System.err.println(e);
        }

        return finalX_;
    }

    private boolean converged(double[] x, double[] x_) {
        double diff = 0;
        for (int i = 0; i < x.length; i++) {
            diff += Math.abs(x[i] - x_[i]);
        }

        return diff < EPSILON;
    }

    private double[] multiply(double[] x, int N) {
        double[] x_ = new double[N];

        double base_score = 0.0;
        for (int j = 0; j < N; j++) {
            if (out[j] == 0) {
                base_score += x[j] * (1.0 / (double) N);
            } else {
                base_score += x[j] * (BORED / (double) N);
                for (Integer i : link.get(j).keySet()) {
                    x_[i] += x[j] * (1.0 - BORED) / (double) out[j];
                }
            }
        }

        for (int i = 0; i < N; i++) {
            x_[i] += base_score;
        }

        return x_;
    }

    double[] algorithm1(int numberOfDocs, int N, double c, boolean print) {
        Random random = new Random();
        double[] x = new double[numberOfDocs];

        // N Random Walks
        for (int n = 0; n < N; n++) {

            // Random Walk
            int currentDoc = random.nextInt(numberOfDocs);
            while (true) {
                double rand = random.nextDouble();

                if (rand >= c) {
                    x[currentDoc] += 1;
                    break;
                } else {
                    // dead end
                    if (out[currentDoc] == 0) {
                        currentDoc = random.nextInt(numberOfDocs);
                    } else {
                        currentDoc = (int) link.get(currentDoc).keySet().toArray()[random.nextInt(out[currentDoc])];
                    }
                }
            }

        }

        for (int i = 0; i < numberOfDocs; i++) {
            x[i] /= N;
        }

        // Sort documents by PageRank
        double[] finalX_ = x;
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(finalX_[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        if (print) {
            // Print top 30
            for (int i = 0; i < 30; i++) {
                System.out.println(sortedPageRankResults.get(i));
            }
        }

        return finalX_;
    }

    double[] algorithm2(int numberOfDocs, int N, double c, boolean print) {
        Random random = new Random();
        double[] x = new double[numberOfDocs];

        int startingDocument = 0;
        // N Random Walks
        for (int n = 0; n < N; n++) {

            // Random Walk
            int currentDoc = startingDocument;
            while (true) {
                double rand = random.nextDouble();

                if (rand >= c) {
                    x[currentDoc] += 1;
                    break;
                } else {
                    // dead end
                    if (out[currentDoc] == 0) {
                        currentDoc = random.nextInt(numberOfDocs);
                    } else {
                        currentDoc = (int) link.get(currentDoc).keySet().toArray()[random.nextInt(out[currentDoc])];
                    }
                }
            }
            startingDocument = (startingDocument + 1) % numberOfDocs;
        }

        for (int i = 0; i < numberOfDocs; i++) {
            x[i] /= N;
        }

        // Sort documents by PageRank
        double[] finalX_ = x;
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(finalX_[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        if (print) {
            // Print top 30
            for (int i = 0; i < 30; i++) {
                System.out.println(sortedPageRankResults.get(i));
            }
        }

        return finalX_;
    }

    double[] algorithm4(int numberOfDocs, int N, double c, boolean print) {
        Random random = new Random();
        double[] x = new double[numberOfDocs];

        int startingDocument = 0;
        int numberOfVisits = 0;
        // N Random Walks
        for (int n = 0; n < N; n++) {

            // Random Walk
            int currentDoc = startingDocument;
            while (true) {
                numberOfVisits++;
                x[currentDoc] += 1;

                double rand = random.nextDouble();
                if (rand >= c) {
                    x[currentDoc] += 1;
                    break;
                } else {
                    // dead end
                    if (out[currentDoc] == 0) {
                        break;
                    } else {
                        currentDoc = (int) link.get(currentDoc).keySet().toArray()[random.nextInt(out[currentDoc])];
                    }
                }

            }

            startingDocument = (startingDocument + 1) % numberOfDocs;
        }

        for (int i = 0; i < numberOfDocs; i++) {
            x[i] /= numberOfVisits;
        }

        // Sort documents by PageRank
        double[] finalX_ = x;
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(finalX_[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        if (print) {
            // Print top 30
            for (int i = 0; i < 30; i++) {
                System.out.println(sortedPageRankResults.get(i));
            }
        }

        return finalX_;
    }

    double[] algorithm5(int numberOfDocs, int N, double c, boolean print) {
        Random random = new Random();
        double[] x = new double[numberOfDocs];

        int numberOfVisits = 0;
        // N Random Walks
        for (int n = 0; n < N; n++) {

            // Random Walk
            int currentDoc = random.nextInt(numberOfDocs);
            while (true) {
                numberOfVisits++;
                x[currentDoc] += 1;

                double rand = random.nextDouble();
                if (rand >= c) {
                    x[currentDoc] += 1;
                    break;
                } else {
                    // dead end
                    if (out[currentDoc] == 0) {
                        break;
                    } else {
                        currentDoc = (int) link.get(currentDoc).keySet().toArray()[random.nextInt(out[currentDoc])];
                    }
                }

            }

        }

        for (int i = 0; i < numberOfDocs; i++) {
            x[i] /= numberOfVisits;
        }

        // Sort documents by PageRank
        double[] finalX_ = x;
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(finalX_[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        if (print) {
            // Print top 30
            for (int i = 0; i < 30; i++) {
                System.out.println(sortedPageRankResults.get(i));
            }
        }

        return finalX_;
    }

    void experiment(int numberOfDocs, int maxIterations, double c) {
        final double[] baseValues = iterate(numberOfDocs, maxIterations);
        List<PageRankResult> sortedPageRankResults = IntStream.range(0, numberOfDocs)
                .mapToObj(i -> new PageRankResult(baseValues[i], docName[i]))
                .sorted()
                .collect(Collectors.toList());

        System.out.println("x axis");
        System.out.print("[1.0");
        for (double m = 3.5; m < 100; m += 2.5) {
            System.out.print("," + m);
        }
        System.out.println("]");

        System.out.println("Experimenting method 1...");
        List<Double> errors = new ArrayList<>();
        List<Double> times = new ArrayList<>();

        for (double m = 1; m < 100; m += 2.5) {
            double mean_error = 0.0;
            double mean_time = 0.0;

            for (int j = 0; j < 10; j++) {
                long startTime = System.currentTimeMillis();
                double[] result = algorithm1(numberOfDocs, (int) (m * numberOfDocs), c, false);
                mean_error += sum_squared_diff(sortedPageRankResults, result, numberOfDocs);
                mean_time += System.currentTimeMillis() - startTime;
            }

            mean_error /= 10;
            mean_time /= 10;
            errors.add(mean_error);
            times.add(mean_time);
        }

        System.out.println("errors");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < errors.size(); i++) {
            System.out.print("," + errors.get(i));
        }
        System.out.println("]");

        System.out.println("times");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < times.size(); i++) {
            System.out.print("," + times.get(i));
        }
        System.out.println("]");

        System.out.println("Experimenting method 2...");
        errors.clear();
        times.clear();

        for (double m = 1; m < 100; m += 2.5) {
            double mean_error = 0.0;
            double mean_time = 0.0;

            for (int j = 0; j < 10; j++) {
                long startTime = System.currentTimeMillis();
                double[] result = algorithm2(numberOfDocs, (int) (m * numberOfDocs), c, false);
                mean_error += sum_squared_diff(sortedPageRankResults, result, numberOfDocs);
                mean_time += System.currentTimeMillis() - startTime;
            }

            mean_error /= 10;
            mean_time /= 10;
            errors.add(mean_error);
            times.add(mean_time);
        }

        System.out.println("errors");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < errors.size(); i++) {
            System.out.print("," + errors.get(i));
        }
        System.out.println("]");

        System.out.println("times");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < times.size(); i++) {
            System.out.print("," + times.get(i));
        }
        System.out.println("]");


        System.out.println("Experimenting method 4...");
        errors.clear();
        times.clear();

        for (double m = 1; m < 100; m += 2.5) {
            double mean_error = 0.0;
            double mean_time = 0.0;

            for (int j = 0; j < 10; j++) {
                long startTime = System.currentTimeMillis();
                double[] result = algorithm4(numberOfDocs, (int) (m * numberOfDocs), c, false);
                mean_error += sum_squared_diff(sortedPageRankResults, result, numberOfDocs);
                mean_time += System.currentTimeMillis() - startTime;
            }

            mean_error /= 10;
            mean_time /= 10;
            errors.add(mean_error);
            times.add(mean_time);
        }

        System.out.println("errors");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < errors.size(); i++) {
            System.out.print("," + errors.get(i));
        }
        System.out.println("]");

        System.out.println("times");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < times.size(); i++) {
            System.out.print("," + times.get(i));
        }
        System.out.println("]");


        System.out.println("Experimenting method 5...");
        errors.clear();
        times.clear();

        for (double m = 1; m < 100; m += 2.5) {
            double mean_error = 0.0;
            double mean_time = 0.0;

            for (int j = 0; j < 10; j++) {
                long startTime = System.currentTimeMillis();
                double[] result = algorithm5(numberOfDocs, (int) (m * numberOfDocs), c, false);
                mean_error += sum_squared_diff(sortedPageRankResults, result, numberOfDocs);
                mean_time += System.currentTimeMillis() - startTime;
            }

            mean_error /= 10;
            mean_time /= 10;
            errors.add(mean_error);
            times.add(mean_time);
        }

        System.out.println("errors");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < errors.size(); i++) {
            System.out.print("," + errors.get(i));
        }
        System.out.println("]");

        System.out.println("times");
        System.out.print("[" + errors.get(0));
        for (int i = 1; i < times.size(); i++) {
            System.out.print("," + times.get(i));
        }
        System.out.println("]");

    }

    double sum_squared_diff(List<PageRankResult> sortedPageRankResults, double[] prediction, int numberOfDocs) {
        double result = 0.0;

        for (int i = 0; i < 30; i++) {
            PageRankResult pageRankResult = sortedPageRankResults.get(i);
            result += Math.pow(pageRankResult.rank - prediction[docNumber.get(pageRankResult.docName)], 2);
        }

        return result;
    }

    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new PageRank(args[0]);
        }
    }
}
