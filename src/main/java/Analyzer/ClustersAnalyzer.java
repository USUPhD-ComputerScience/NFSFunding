package Analyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import Managers.WordManager;
import Models.Proposal;
import Models.Word;
import NLP.WordVec;

public class ClustersAnalyzer {
	public static final int YEARS = 22;

	public static void clusterWords(String outputFile, Collection<Word> words,
			String timeSeriesFile, Set<Proposal> proposalSet) throws Throwable {
		System.out.println("> Read data from test files");
		ArrayList<Item> itemSet = new ArrayList<>();
		for (Word w : words) {
			Item it = new Item(w.toString(), w.getTotalCount());
			if (it.getVector() == null)
				continue;
			itemSet.add(it);
		}
		cluster(new File(outputFile), itemSet, new File(timeSeriesFile),
				proposalSet);
		System.out.println("> Done!");
	}

	public static long[][] getTimeSeries(List<String> bagOfWords,
			Set<Proposal> proposalSet) {
		WordManager wordMan = WordManager.getInstance();
		long[] countSeries = new long[YEARS];
		long[] moneySeries = new long[YEARS];

		for (Proposal proposal : proposalSet) {
			int year = proposal.getYear();
			if (year < 1993 || year > 2014)
				continue;
			if (proposal.isContainWordsFromSet(bagOfWords,0.25)) {
				countSeries[year - 1993] += 1;
				moneySeries[year - 1993] += proposal.getAmount();
			}
		}
		long[][] result = new long[2][];
		result[0] = countSeries;
		result[1] = moneySeries;
		return result;
	}

	private static void cluster(File file, ArrayList<Item> words,
			File timeSeriesFile, Set<Proposal> proposalSet) throws Throwable {

		List<Clusterable> itemList = new ArrayList<>();
		if (words.isEmpty())
			return;
		for (Item word : words)
			itemList.add(word);
		List<List<Clusterable>> clusters = KMeanClustering.clusterBySimilarity(
				100, itemList, (int) Math.round(Math.sqrt(words.size() / 2)),
				KMeanClustering.COSINE_SIM);
		// List<List<Clusterable>> clusters =
		// KMeanClustering.clusterBySimilarity(
		// 100, itemList, 0.4, KMeanClustering.COSINE_SIM);
		System.out.println("> Write clusters to file");
		writeClustersToFile(clusters, file, timeSeriesFile, proposalSet);
	}

	private static void writeClustersToFile(List<List<Clusterable>> clusters,
			File file, File timeSeriesFile, Set<Proposal> proposalSet)
					throws Throwable {
		PrintWriter pw = new PrintWriter(new FileWriter(file));
		PrintWriter pwTimeS = new PrintWriter(new FileWriter(timeSeriesFile));
		int topic = 0;
		pwTimeS.print("Topic,");
		for (int i = 0; i < YEARS; i++) {
			int year = 1993 + i;
			pwTimeS.print(year);
			if (i != YEARS - 1)
				pwTimeS.print(",");
			else
				pwTimeS.println();
		}
		for (List<Clusterable> cluster : clusters) {
			topic++;
			List<String> bagOfWords = new ArrayList<>();
			if (cluster.isEmpty())
				continue;
			Item mainTopic = (Item) cluster.get(0);
			pw.print(mainTopic.toString() + ",");
			int totalCount = 0;
			String top = "";
			String second = "";
			double topscore = 0.0;
			for (Clusterable item : cluster) {
				Item word = (Item) item;
				pw.print("<" + word.toString() + ">");
				int frq = word.getFrequency();
				totalCount += frq;
				if (topscore < frq) {
					topscore = frq;
					second = top;
					top = word.toString();
				}
				bagOfWords.add(word.toString());
			}
			pw.println("," + totalCount + "," + top + "_" + second + ","
					+ topscore);
			long[][] timeS = getTimeSeries(bagOfWords, proposalSet);
			pwTimeS.print("cluster" + topic + "'s amount,");
			for (int i = 0; i < YEARS; i++) {
				pwTimeS.print(timeS[1][i]);
				if (i != YEARS - 1)
					pwTimeS.print(",");
				else
					pwTimeS.println();
			}
			pwTimeS.print("cluster" + topic + "'s count,");
			for (int i = 0; i < YEARS; i++) {
				pwTimeS.print(timeS[0][i]);
				if (i != YEARS - 1)
					pwTimeS.print(",");
				else
					pwTimeS.println();
			}
			double[] countGrowths = getTimeSeriesGrowthRate(timeS[1]);
			pwTimeS.print("cluster" + topic + "'s amount Growth Rate,");
			for (int i = 0; i < YEARS; i++) {
				pwTimeS.print(countGrowths[i]);
				if (i != YEARS - 1)
					pwTimeS.print(",");
				else
					pwTimeS.println();
			}
			double[] amountGrowths = getTimeSeriesGrowthRate(timeS[0]);
			pwTimeS.print("cluster" + topic + "'s count Growth Rate,");
			for (int i = 0; i < YEARS; i++) {
				pwTimeS.print(amountGrowths[i]);
				if (i != YEARS - 1)
					pwTimeS.print(",");
				else
					pwTimeS.println();
			}
		}
		pw.close();
		pwTimeS.close();
	}

	// ARIMA model
	private static double[] getTimeSeriesGrowthRate(long[] timeseries) {
		double[] growthRate = new double[timeseries.length];
		long lastValue = timeseries[0];
		growthRate[0] = 0;
		for (int i = 1; i < timeseries.length; i++) {
			if (timeseries[i] == lastValue && lastValue == 0) {
				growthRate[i] = 0;
			} else {
				if (lastValue == 0)
					growthRate[i] = 100;
				else
					growthRate[i] = 100 * ((double)timeseries[i] / lastValue) - 100;
			}
			lastValue = timeseries[i];
		}
		return growthRate;
	}
	// private static void loadTestSet(File file) throws FileNotFoundException {
	// // TODO Auto-generated method stub
	// words = new ArrayList<>();
	// Scanner br = new Scanner(new FileReader(file));
	// String first = br.nextLine();
	// Set<String> stopwords = NatureLanguageProcessor.getInstance()
	// .getStopWordSet();
	// int count = 0;
	// while (br.hasNextLine()) {
	// String[] values = br.nextLine().split(",");
	// if (values.length == 10) {
	// String word = values[0];
	// if (stopwords.contains(word))
	// continue;
	// if (Util.isNumeric(word))
	// continue;
	// double ratio = (Double.parseDouble(values[1]) + Double
	// .parseDouble(values[2]))
	// / (Double.parseDouble(values[4]) + Double
	// .parseDouble(values[5]));
	// if (ratio <= 1.2)
	// continue;
	// // if (Double.parseDouble(values[9]) <= 0.5)
	// // continue;
	// count++;
	// int ratioXDiff = (int) Double.parseDouble(values[9]);
	// Item item = new Item(word, ratioXDiff);
	// if (item.getVector() != null)
	// words.add(item);
	// }
	// }
	// br.close();
	// System.out.println(">> Read " + count + " words!");
	// }

	private static class Item extends Clusterable {

		float[] vector = null;
		int frequency;
		String word;
		boolean change = false;
		public static final WordVec word2vec = new WordVec();

		public Item(String str, int freq) {
			// TODO Auto-generated constructor stub
			frequency = freq;
			word = str.intern();
			float[] tempVector = word2vec.getVectorForWord(word);
			if (tempVector != null) {
				vector = new float[WordVec.VECTOR_SIZE];
				for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
					vector[i] = tempVector[i];
				}
			}
		}

		public String toString() {
			return word;
		}

		@Override
		public float[] getVector() {
			// TODO Auto-generated method stub
			return vector;
		}

		@Override
		public int getFrequency() {
			// TODO Auto-generated method stub
			return frequency;
		}

		@Override
		public void setChange(boolean isChanged) {
			// TODO Auto-generated method stub
			change = isChanged;
		}

		@Override
		public boolean isChanged() {
			// TODO Auto-generated method stub
			return change;
		}

	}
}
