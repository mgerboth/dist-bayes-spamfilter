
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class BayesSpamfilter {

    private static final float PROBABILITY_OF_SPAM = 0.7f;

    private static final float PROBABILITY_OF_HAM = 0.3f;

    public static final float THRESHOLD = 0.5f;

    public static final float ALPHA = 0.02f;

    static Map<String, Integer> spamBibliography;
    static Map<String, Integer> hamBibliography;


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String hamPath = "./src/main/resources/ham-anlern";
        String spamPath = "./src/main/resources/spam-anlern";

        hamBibliography = getWordBibliography(hamPath);
        spamBibliography = getWordBibliography(spamPath);

        balanceBibliographies();

        String hamTest = "./src/main/resources/ham-test";
        String spamTest = "./src/main/resources/spam-test";

        float spamInSpam = getNumberOfSpamInDirectory(spamTest);
        float spamInHam = getNumberOfSpamInDirectory(hamTest);

        float filesInSpam = getNumberOfFilesInDirectory(spamTest);
        float filesInHam = getNumberOfFilesInDirectory(hamTest);

        float percentageOfSpamInSpam = spamInSpam / filesInSpam * 100;
        float percentageOfSpamInHam = spamInHam / filesInHam * 100;


        System.out.println(percentageOfSpamInHam + "% Spam in '" + hamTest + "'.");
        System.out.println(percentageOfSpamInSpam + "% Spam in '" + spamTest + "'.");
        System.out.println("Alpha = " + ALPHA + ", Schwellenwert = " + THRESHOLD);


    }

    /**
     *
     */
    private static void balanceBibliographies() {
        for (String word : hamBibliography.keySet()) {
            if (!spamBibliography.containsKey(word)) {
                spamBibliography.put(word, 1);
            }
        }
        for (String word : spamBibliography.keySet()) {
            if (!hamBibliography.containsKey(word)) {
                hamBibliography.put(word, 1);
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
    private static String normalizeText(String text){
        String escaped = text.replaceAll("[^A-Za-z]", " ").toLowerCase();
        return escaped;
    }

    /**
     * @param directoryPath
     * @return Map with normalized words of each file in the directory
     * @throws IOException
     */
    private static Map<String, Integer> getWordBibliography(String directoryPath) throws IOException {
        Map<String, Integer> bibliography = new HashMap<String, Integer>();
        //Map<String, Integer> limitedBibliography = new HashMap<String, Integer>();

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
                            bibliography.put(word, 1);
                        }
                    }
                }
            }
        }

        return bibliography;
    }


    /**
     * @param filePath
     * @return
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
     * @return the probability of a word beeing spam
     */
    private static float getSpamProbabilityOfWord(String word) {
        float prWS = (float) spamBibliography.get(word) / (float) spamBibliography.keySet().size();
        float prWH = (float) hamBibliography.get(word) / (float) hamBibliography.keySet().size();

        // bayes formula for probability
        float res = (prWS * PROBABILITY_OF_SPAM) / ((prWS * PROBABILITY_OF_SPAM) + (prWH * PROBABILITY_OF_HAM));
        return res;
    }
}