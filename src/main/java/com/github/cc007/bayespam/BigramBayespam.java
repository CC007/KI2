package com.github.cc007.bayespam;

import java.io.*;
import java.util.*;
import org.apache.commons.io.IOUtils;

public class BigramBayespam {

    // This defines the two types of messages we have.
    static enum MessageType {

        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter {

        int counter_spam = 0;
        int counter_regular = 0;

        /// conditional likelyhood storage for spam and regular
        double conditional_likeyhood_spam;
        double conditional_likeyhood_regular;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type) {
            if (type == MessageType.NORMAL) {
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    /// epsilon for calculating probability
    private static final double epsilon = 0.001;

    /// additional parameters
    private static final int minWordSize = 4;
    private static final int frequencyThreshold = 1;

    /// prior probability storgae for regular and spam
    private static double priorRegular;
    private static double priorSpam;

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static final Hashtable<String, Multiple_Counter> vocab = new Hashtable<>();

    // Add a word to the vocabulary
    private static void addBigram(String bigram, MessageType type) {
        Multiple_Counter counter = new Multiple_Counter();

        if (vocab.containsKey(bigram)) {                  // if word exists already in the vocabulary..
            counter = vocab.get(bigram);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(bigram, counter);                       // put the word with its counter into the hashtable
    }

    // List the regular and spam messages
    private static void listDirs(File dir_location) {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if (dir_listing.length != 2) {
            System.out.println("- Error: specified directory does not contain two subdirectories.\n");
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam = dir_listing[1].listFiles();
    }

    // Print the current content of the vocabulary
    private static void printVocab() {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys(); e.hasMoreElements();) {
            String word;

            word = e.nextElement();
            counter = vocab.get(word);

            System.out.println(word + " | in regular: " + counter.counter_regular
                    + " in spam: " + counter.counter_spam);
        }
    }

    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type)
            throws IOException {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL) {
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }

        for (int i = 0; i < messages.length; ++i) {
            FileInputStream i_s = new FileInputStream(messages[i]);
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String token;
            String previousToken = "";

            while ((line = in.readLine()) != null) // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens()) // while there are stille words left..
                {
                    /// I set everything to lower case and filter all non-letter characters from the text
                    token = st.nextToken().toLowerCase().replaceAll("[^a-z]", "");
                    /// I only add bigrams with words longer than or equal to 4 characters
                    if (token.length() >= minWordSize) {
                        ///nothing to add if there is only one token, since this concerns bigrams
                        if (!"".equals(previousToken)) {
                            addBigram(previousToken + " " + token, type);                  // add them to the vocabulary
                        }
                        previousToken = token;
                    }
                }
            }

            in.close();
        }
    }

    /// This function tries to classify the files dependent on the priors and conditional likelyhoods
    private static MessageType classify(File file)
            throws IOException {
        FileInputStream i_s = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
        String line;
        String token;
        String previousToken = "";

        /// Initialize probabilities to the prior log values
        double pRegular = priorRegular;
        double pSpam = priorSpam;
        while ((line = in.readLine()) != null) // read a line
        {
            StringTokenizer st = new StringTokenizer(line);         // parse it into words

            while (st.hasMoreTokens()) // while there are stille words left..
            {
                /// I set everything to lower case and filter all non-letter characters from the text
                token = st.nextToken().toLowerCase().replaceAll("[^a-z]", "");
                /// If the word is in the vocab, add the conditional likelyhood log value to the probability
                if (vocab.containsKey(previousToken + " " + token)) {
                    pRegular += vocab.get(previousToken + " " + token).conditional_likeyhood_regular;
                    pSpam += vocab.get(previousToken + " " + token).conditional_likeyhood_spam;
                }
                previousToken = token;
            }
        }

        in.close();
        return pRegular > pSpam ? MessageType.NORMAL : MessageType.SPAM;
    }

    public static void main(String[] args)
            throws IOException {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File(args[0]);

        // Check if the cmd line arg is a directory
        if (!dir_location.isDirectory()) {
            System.out.println("- Error: cmd line arg 1 not a directory.\n");
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        double nMessagesRegular = listing_regular.length;
        double nMessagesSpam = listing_spam.length;
        double nMessagesTotal = nMessagesRegular + nMessagesSpam;

        /// Calculate priors (log)
        priorRegular = nMessagesRegular / nMessagesTotal;
        priorRegular = Math.log(priorRegular);
        priorSpam = nMessagesSpam / nMessagesTotal;
        priorSpam = Math.log(priorSpam);

        /// Count words
        double nBigramsRegular = 0;
        double nBigramsSpam = 0;
        for (Multiple_Counter counter : vocab.values()) {
            nBigramsRegular += counter.counter_regular;
            nBigramsSpam += counter.counter_spam;
        }

        /// Calculate conditional likelyhoods (log)
        for (Multiple_Counter counter : vocab.values()) {
            counter.conditional_likeyhood_regular = counter.counter_regular == 0
                    ? epsilon / (nBigramsRegular + nBigramsSpam)
                    : counter.counter_regular / nBigramsRegular;
            counter.conditional_likeyhood_regular = Math.log(counter.conditional_likeyhood_regular);
            counter.conditional_likeyhood_spam = counter.counter_spam == 0
                    ? epsilon / (nBigramsRegular + nBigramsSpam)
                    : counter.counter_spam / nBigramsSpam;
            counter.conditional_likeyhood_spam = Math.log(counter.conditional_likeyhood_spam);
        }
        List<String> vocabKeys = new ArrayList<>();
        vocabKeys.addAll(vocab.keySet());
        for (String key : vocabKeys) {
            if (vocab.get(key).counter_regular + vocab.get(key).counter_spam < frequencyThreshold) {
                vocab.remove(key);
            }
        }
        System.out.println("Total amount of keys after trim: " + vocab.keySet().size());

        // Print out the hash table
        printVocab();
        // Location of the directory (the path) taken from the cmd line (second arg)
        dir_location = new File(args[1]);

        // Check if the cmd line arg is a directory
        if (!dir_location.isDirectory()) {
            System.out.println("- Error: cmd line arg 2 not a directory.\n");
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        /// calculate good and bad classifications for both regular and spam
        int regularGood = 0;
        int regularBad = 0;
        int spamGood = 0;
        int spamBad = 0;
        for (File regularFile : listing_regular) {
            /// Print the file, just because I can
            System.out.println("File: " + regularFile.getName());
            System.out.println(IOUtils.toString(new FileInputStream(regularFile)));
            if (classify(regularFile) == MessageType.NORMAL) {
                regularGood++;
            } else {
                regularBad++;
            }
        }
        for (File spamFile : listing_spam) {
            /// Print the file, just because I can
            System.out.println("File: " + spamFile.getName());
            System.out.println(IOUtils.toString(new FileInputStream(spamFile)));
            if (classify(spamFile) == MessageType.SPAM) {
                spamGood++;
            } else {
                spamBad++;
            }

        }/// print the confusion matrix
        System.out.println("               | Regular classified | Spam classified");
        System.out.println("Actual regular | " + String.format("%1$18s", regularGood) + " | " + String.format("%1$15s", regularBad));
        System.out.println("Actual Spam    | " + String.format("%1$18s", spamBad) + " | " + String.format("%1$15s", spamGood));

        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}
