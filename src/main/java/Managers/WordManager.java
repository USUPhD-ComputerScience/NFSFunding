package Managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import Analyzer.ClustersAnalyzer;
import Models.Proposal;
import Models.Word;

public class WordManager {
	private static WordManager instance = null;
	private Map<String,Word> wordSet = new HashMap<>();
	public static WordManager getInstance(){
		if(instance == null)
			instance = new WordManager();
		return instance;
	}
	private WordManager() {
		// TODO Auto-generated constructor stub
	} 
	public Word addWord(String word, int year, long money){
		Word exist = wordSet.get(word);
		if(exist == null){
			exist = new Word(word,year,money);
			wordSet.put(word, exist);
		}else{
			exist.addCount(year);
			exist.addMoney(year, money);
			wordSet.put(word,exist);
		}
		return exist;
	}
	public Word getWord(String word){
		return wordSet.get(word);
	}
	public void getStatistic(String outputFile, String timeSeriesFile,Set<Proposal>proposalSet) throws Throwable{
		System.out.println("---- Statistic -----");
		System.out.println("Number of words: " + wordSet.size());
		System.out.print("Some words: ");
		int i = 0;
		for(String word : wordSet.keySet()){
			System.out.print(word + ", ");
			i++;
			if(i == 10)
				break;
		}
		ClustersAnalyzer.clusterWords(outputFile, wordSet.values(), timeSeriesFile, proposalSet);
	}
}
