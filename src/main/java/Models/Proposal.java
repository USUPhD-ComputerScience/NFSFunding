package Models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Managers.WordManager;
import NLP.NatureLanguageProcessor;

public class Proposal {
	private Set<String> wordSet;
	private int mYear;
	private long mAmount;
	public Proposal(String text, int year, long amount) {
		// TODO Auto-generated constructor stub
		mYear = year;
		mAmount = amount;
		wordSet = processProposal(text, year, amount);
	}
	public static Set<String> processProposal(String text, int year, long amount) {
		Set<String> results = new HashSet<>();
		WordManager wordman = WordManager.getInstance();
		Set<String> stopwords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		Set<String> bagOfWords = NatureLanguageProcessor.getInstance()
				.extractBagOfWords(text);
		for (String w : bagOfWords) {
			if (w.length() <= 2 || stopwords.contains(w)) {
				continue;
			}
			results.add(wordman.addWord(w, year, amount).toString().intern());
		}
		return results;
	}
	public int getYear(){
		return mYear;
	}
	public long getAmount(){
		return mAmount;
	}
	public boolean isContainWordsFromSet(List<String> bagOfWords, double ratio){
		int matchCount = 0;
		int totalWords = wordSet.size();
		for(String w: bagOfWords){
			if(wordSet.contains(w)){
				matchCount++;
				double rat = (double) matchCount / totalWords;
				if(rat >= ratio)
					return true;
			}
		}
		return false;
	}
}
