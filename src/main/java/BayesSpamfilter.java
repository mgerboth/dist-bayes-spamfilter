
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class BayesSpamfilter {

    private static final float PROBABILITY_OF_SPAM = 0.75f;
    private static final float PROBABILITY_OF_HAM = 1 - PROBABILITY_OF_SPAM;
    private static final float THRESHOLD = 0.5f;
    private static final float ALPHA = 0.02f;

    private static String HAM_PATH = "./src/main/resources/ham-anlern";
    private static String SPAM_PATH = "./src/main/resources/spam-anlern";

    private static String HAM_TEST = "./src/main/resources/ham-test";
    private static String SPAM_TEST = "./src/main/resources/spam-test";

    private static String HAM_CALIBRATION = "./src/main/resources/ham-kallibrierung";
    private static String SPAM_CALIBRATION = "./src/main/resources/spam-kallibrierung";


    private static Map<String, Float> spamBibliography;
    private static Map<String, Float> hamBibliography;


    /**
     * Main Method to run the BayesSpamfilter, shows the evaluation results in the terminal.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        spamBibliography = new HashMap<>();
        hamBibliography = new HashMap<>();

        setWordBibliography(HAM_PATH, hamBibliography);
        setWordBibliography(SPAM_PATH, spamBibliography);

        // Balance Bibliography Librarys after anlern-phase
        balanceBibliographies();

        float filesInSpam = getNumberOfFilesInDirectory(SPAM_TEST);
        float filesInHam = getNumberOfFilesInDirectory(HAM_TEST);

        float spamInSpam = getNumberOfSpamInDirectory(SPAM_TEST);
        float spamInHam = getNumberOfSpamInDirectory(HAM_TEST);

        float percentageOfSpamInSpam = spamInSpam / filesInSpam * 100;
        float percentageOfSpamInHam = spamInHam / filesInHam * 100;

        System.out.println("########### lern with anlern files ############");
        System.out.println("Spam as Spam detected: " + (int) spamInSpam + " / " + (int) filesInSpam + "  ## Spam in Ham detected: " + (int) spamInHam + " / " + (int) filesInHam);
        System.out.println(percentageOfSpamInHam + "% Spam in '" + HAM_TEST + "'.");
        System.out.println(percentageOfSpamInSpam + "% Spam in '" + SPAM_TEST + "'.");

        // Run Calibration

        setWordBibliography(HAM_CALIBRATION, hamBibliography);
        setWordBibliography(SPAM_CALIBRATION, spamBibliography);

        // Rebalance after the calibration
        balanceBibliographies();

        // Re-Run Tests after Calibration
        spamInSpam = getNumberOfSpamInDirectory(SPAM_TEST);
        spamInHam = getNumberOfSpamInDirectory(HAM_TEST);

        percentageOfSpamInSpam = spamInSpam / filesInSpam * 100;
        percentageOfSpamInHam = spamInHam / filesInHam * 100;

        System.out.println();
        System.out.println("########### improve algorithm with calibration files ############");
        System.out.println("Spam as Spam detected: " + (int) spamInSpam + " / " + (int) filesInSpam + "  ## Spam in Ham detected: " + (int) spamInHam + " / " + (int) filesInHam);
        System.out.println(percentageOfSpamInHam + "% Spam in '" + HAM_TEST + "'.");
        System.out.println(percentageOfSpamInSpam + "% Spam in '" + SPAM_TEST + "'.");
        System.out.println();
        System.out.println("Alpha = " + ALPHA + ", Schwellenwert = " + THRESHOLD);
    }

    /**
     * 2b) balances the generated Bibliographie lists so that missing words are added
     * in the opposite Bibliographie library. ALPHA is used for masking these words.
     */
    private static void balanceBibliographies() {
        for (String word : hamBibliography.keySet()) {
            if (!spamBibliography.containsKey(word)) {
                spamBibliography.put(word, ALPHA);
            }
        }
        for (String word : spamBibliography.keySet()) {
            if (!hamBibliography.containsKey(word)) {
                hamBibliography.put(word, ALPHA);
            }
        }
    }

    /**
     * @param directoryPath
     * @return the number of Spam in the directory
     * @throws IOException
     */
    private static int getNumberOfSpamInDirectory(String directoryPath) throws IOException {
        int nOfSpam = 0;

        File directory = new File(directoryPath);
        for (File file : directory.listFiles()) {
            if (getSpamProbabilityOfFile(file.getAbsolutePath()).floatValue() > THRESHOLD) {
                nOfSpam++;
            }
        }

        return nOfSpam;
    }

    /**
     * @param directoryPath
     * @return the number of files in the directory
     */
    private static int getNumberOfFilesInDirectory(String directoryPath) {
        int nOfFiles = 0;

        File directory = new File(directoryPath);
        for (File file : directory.listFiles()) {
            nOfFiles++;
        }

        return nOfFiles;
    }

    /**
     * @param text
     * @return a normalized word to only lowercase chars
     */
    private static String normalizeText(String text) {
        String escaped = text.replaceAll("[^A-Za-z]", " ").toLowerCase();
        return escaped;
    }

    /**
     * @param directoryPath
     * @return Map with normalized words of each file in the directory
     * @throws IOException
     */
    private static void setWordBibliography(String directoryPath, Map<String, Float> bibliography) throws IOException {
        // Find all words and put them in map
        File directory = new File(directoryPath);
        for (File file : directory.listFiles()) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String text;
            //
            while ((text = bufferedReader.readLine()) != null) {
                String escaped = normalizeText(text);
                for (String word : escaped.split(" ")) {
                    if (!("".equals(word))) {
                        if (bibliography.containsKey(word)) {
                            bibliography.put(word, bibliography.get(word) + 1);
                        } else {
                            bibliography.put(word, 1.0f);
                        }
                    }
                }
            }
        }
    }


    /**
     * @param filePath
     * @return the probability of the whole file that is being tested. Cals for each word in file the
     * method getSpamProbabilityOfWord and sums the probability up with the formula stated in wiki: "Combining individual probabilities"
     * @throws IOException
     */
    private static BigDecimal getSpamProbabilityOfFile(String filePath) throws IOException {
        File file = new File(filePath);

        BufferedReader br = new BufferedReader(new FileReader(file));
        BigDecimal compProbabilityNum = new BigDecimal(1);
        BigDecimal compProbabilityDenum = new BigDecimal(1);

        String text;
        while ((text = br.readLine()) != null) {
            String escaped = normalizeText(text);
            // Calc probability for each word in file
            for (String word : escaped.split(" ")) {
                if (spamBibliography.containsKey(word) && hamBibliography.containsKey(word) && !("".equals(word))) {
                    float probabilityOfWord = getSpamProbabilityOfWord(word);
                    compProbabilityNum = compProbabilityNum.multiply(new BigDecimal(Float.toString(probabilityOfWord)));
                    compProbabilityDenum = new BigDecimal(1).subtract(new BigDecimal(Float.toString(probabilityOfWord)))
                            .multiply(compProbabilityDenum);
                }
            }
        }

        // Calc probability of the whole file
        BigDecimal denominator = compProbabilityNum.add(compProbabilityDenum);
        BigDecimal res = compProbabilityNum.divide(denominator, RoundingMode.HALF_UP);
        return res;
    }

    /**
     * @param word
     * @return the probability of a word being spam after the definition of bayes formula
     */
    private static float getSpamProbabilityOfWord(String word) {
        float prWS = (float) spamBibliography.get(word) / (float) spamBibliography.keySet().size();
        float prWH = (float) hamBibliography.get(word) / (float) hamBibliography.keySet().size();

        // bayes formula for probability
        float res = (prWS * PROBABILITY_OF_SPAM) / ((prWS * PROBABILITY_OF_SPAM) + (prWH * PROBABILITY_OF_HAM));
        return res;
    }
}