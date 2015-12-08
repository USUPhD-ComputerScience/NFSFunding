package NLP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Util.Util;
import au.com.bytecode.opencsv.CSVReader;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class NatureLanguageProcessor {
	public static final String[] POSLIST = { "UH", "VB", "VBD", "VBG", "VBN",
			"VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "$", "``", "NNPS", "NNS",
			"PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
			"CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD",
			"NN", "NNP" };

	public static final Set<String> VERB_TAG = new HashSet<>(
			Arrays.asList(new String[] { "VBG", "VBP", "VB", "VP", "@VP",
					"VBZ", "VBN" }));
	public static final Set<String> NOUN_TAG = new HashSet<>(Arrays
			.asList(new String[] { "NN", "NNS", "NP", "PRP", "PRP$" }));
	
	public static final Set<String> POSSET = new HashSet<>(
			Arrays.asList(POSLIST));
	private Set<String> stopWordSet;// less extensive

	public Set<String> getStopWordSet() {
		return stopWordSet;
	}

	private static NatureLanguageProcessor instance = null;
	MaxentTagger PoSTagger;

	public static synchronized NatureLanguageProcessor getInstance() {
		if (instance == null)
			instance = new NatureLanguageProcessor();
		return instance;
	}

	private NatureLanguageProcessor() {
		readStopWordsFromFile();
		PoSTagger = new MaxentTagger(
				"lib/dictionary/POS/english-left3words-distsim.tagger");
		try {
			//Porter2StemmerInit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readStopWordsFromFile() {
		stopWordSet = new HashSet<>();
		System.err
				.println(">Read StopWords from file - englishImprovised.stop");
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(
					"lib/dictionary/stop/englishImprovised.stop"));
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				stopWordSet.add(row[0]);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/**
	 * Standardize the text and then split it into sentences, separated by DOT
	 * 
	 * @param text
	 *            - a text
	 * @return a String array of all the sentences.
	 */
	public static String[] extractSentence(String text) {
		return text.split("\\.+|\\?+|!+");
	}

	/**
	 * Return the index of the corresponding PoS tag in the list provided by
	 * this Class. This provides a way to reduce the memory for String objects.
	 * Instead of storing the String of PoS tag, we can store its index.
	 * 
	 * @param PoS
	 *            - a PoS tag
	 * @return the index of that PoS tag or -1 if it is not in the list
	 */
	public boolean checkValidityOfPOS(String PoS) {
		return POSSET.contains(PoS);
	}

	public String cleanseText(String text, boolean keepVN) {
		StringBuilder result = new StringBuilder();
		CustomStemmer stemmer = CustomStemmer.getInstance();

		text = text.toLowerCase();
		// extract sentences
		String[] sentenceSet = extractSentence(text);
		
		// stem words
		for (String sentence : sentenceSet) {
			sentence = Util.removeNonChars(sentence);
			String taggedString = PoSTagger.tagString(sentence);
			String[] taggedWordSet = taggedString.split(" ");
			int count = 0;
			for (String taggedWord : taggedWordSet) {
				String[] pair = taggedWord.split("_");
				if (pair.length < 2)
					continue; // dont stem stopwords
				pair[1] = Util.removeNonChars(pair[1]);
				if(!VERB_TAG.contains(pair[1]) && !NOUN_TAG.contains(pair[1]))
					continue;
				if (!stopWordSet.contains(pair[0]))
					pair = stemmer.stem(pair);
				result.append(pair[0] + " ");
				count++;
			}
			if (count > 0)
				result.replace(result.length(), result.length(), ".");
		}
		return result.toString();
	}

	/**
	 * This function will stem the words in the input List using Porter2/English
	 * stemmer and replace the String value of that word with the stemmed
	 * version.
	 * 
	 * @param wordList
	 *            - a List contains a String array of 2 elements: 0-word, 1-PoS
	 */
	public List<String[]> stem(List<String[]> wordList) {
		List<String[]> results = new ArrayList<>();
		CustomStemmer stemmer = CustomStemmer.getInstance();
		for (String[] pair : wordList) {
			if (pair.length < 2)
				continue;
			// System.out.print(count++ + ": " + pair[0]);
			if (!stopWordSet.contains(pair[0]))
				pair = stemmer.stem(pair);

			results.add(pair);
			// System.out.println("-" + pair[0] + "<->" + pair[1]);
		}
		return results;
	}
	public Set<String> extractBagOfWords(String text){
		Set<String> wordSet = new HashSet<>();
		Pattern pattern = Pattern.compile("\\w+");
		java.util.regex.Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String w = matcher.group();
			wordSet.add(w);
		}
		return wordSet;
	}
	
}
