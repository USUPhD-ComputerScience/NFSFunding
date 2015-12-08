package usu.datascience.ASEFunding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;

import Analyzer.ClustersAnalyzer;
import Managers.WordManager;
import Models.Proposal;
import Models.Word;
import NLP.NatureLanguageProcessor;
import Util.Util;

/**
 * Hello world!
 *
 */
public class App {

	private static Set<Proposal> proposalSet = new HashSet<>();

	public static void main(String[] args) throws Throwable {

		prepareData("D:\\projects\\fundingTrends", "support/correctionMap.txt",true);
		// readTopics("D:\\projects\\NSFFunding\\analizedData\\\10topics");
		// readTopicsSeparateFiles(
		// "D:\\projects\\NSFFunding\\analizedData\\10topics");
		WordManager.getInstance().getStatistic(
				"D:\\projects\\NSFFunding\\analizedData\\VectorClusters.csv",
				"D:\\projects\\NSFFunding\\analizedData\\ClusterTimeseries.csv",
				proposalSet);
	}

	public static void prepareData(String directory, String correctionFile, boolean keepVN)
			throws Throwable {
		Map<String, String> corrector = readCorrectionMap(correctionFile);
		List<String> fileNames = Util.listFilesForFolder(directory);
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// PrintWriter outputCorpus = new PrintWriter(
		// "D:\\projects\\NSFFunding\\cleansedCorpus.txt");

		int count = 0;
		for (String file : fileNames) {
			File fXmlFile = new File(file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();
			try {
				String title = doc.getElementsByTagName("AwardTitle").item(0)
						.getTextContent().toLowerCase();
				int awardAmount = Integer
						.parseInt(doc.getElementsByTagName("AwardAmount")
								.item(0).getTextContent());
				if (awardAmount <= 0)
					continue;
				String abstraction = Jsoup
						.parse(doc.getElementsByTagName("AbstractNarration")
								.item(0).getTextContent())
						.text().toLowerCase();
				String startDate = doc.getElementsByTagName("StartDate").item(0)
						.getTextContent();
				int year = Integer.parseInt(startDate.split("/")[2]);
				long dateLong = Util.normalizeDate(startDate);
				String directorate = Jsoup
						.parse(doc.getElementsByTagName("Directorate").item(0)
								.getTextContent())
						.text().toLowerCase();
				directorate = Util.removeNonChars(directorate);
				String correction = null;
				correction = corrector.get(directorate);
				if (correction != null)
					directorate = correction;
				String division = Jsoup
						.parse(doc.getElementsByTagName("Division").item(0)
								.getTextContent())
						.text().toLowerCase();
				division = Util.removeNonChars(division);
				correction = null;
				correction = corrector.get(division);
				if (correction != null)
					division = correction;
				String cleansedText = nlp.cleanseText(title, keepVN) + " "
						+ nlp.cleanseText(abstraction, keepVN);
				proposalSet.add(new Proposal(cleansedText, year, awardAmount));
				// outputCorpus.println(cleansedText);
				count++;
				if (count % 1000 == 0)
					System.out.println("processed " + count);
			} catch (Exception e) {
				System.err.println(e.getStackTrace());
			}
		}
		System.out.println("processed " + count +"/"+fileNames.size());
		// outputCorpus.close();
		// WordManager.getInstance().getStatistic();
	}

	public static void readTopicsSeparateFiles(String directory)
			throws Throwable {
		Set<String> stopwords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		List<List<String>> topics = new ArrayList<>();

		List<String> fileNames = Util.listFilesForFolder(directory);
		for (String fileName : fileNames) {
			List<String> currentTopic = new ArrayList<>();
			InputStream fis = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(fis,
					Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				String[] row = line.split(",");
				if (row.length != 3)
					continue;
				String w = Util.keepLowerCase(row[1]);
				if (w.length() <= 2 || stopwords.contains(w)) {
					continue;
				}
				currentTopic.add(w);
			}

			br.close();
			topics.add(currentTopic);
		}

		for (List<String> bag : topics) {
			int size = bag.size();
			int endPoint = 50;
			if (endPoint > size)
				endPoint = size;
			System.out.println("--------TOPIC------");
			for (int i = 0; i < endPoint; i++) {
				System.out.print(bag.get(i) + " ");
			}
			System.out.println();
		}
	}

	public static void readTopics(String fileName) throws Throwable {
		Set<String> stopwords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		List<List<String>> topics = new ArrayList<>();
		InputStream fis = new FileInputStream(fileName);
		InputStreamReader isr = new InputStreamReader(fis,
				Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		int numberOfTopic = 0;
		if (line != null) {
			String[] row = line.split(",");
			numberOfTopic = row.length - 1;
			for (int i = 0; i < numberOfTopic; i++) {
				topics.add(new ArrayList<>());
			}
			while ((line = br.readLine()) != null) {
				row = line.split(",");
				if (row.length != numberOfTopic + 1) {
					System.out.println("ALERT!!!!");
					continue;
				}
				for (int i = 0; i < numberOfTopic; i++) {
					String w = Util.keepLowerCase(row[i + 1]);
					if (w.length() <= 2 || stopwords.contains(w)) {
						continue;
					}
					topics.get(i).add(w);
				}
			}
		}
		br.close();
		for (List<String> bag : topics) {
			int size = bag.size();
			int endPoint = 50;
			if (endPoint > size)
				endPoint = size;
			System.out.println("--------TOPIC------");
			for (int i = 0; i < endPoint; i++) {
				System.out.print(bag.get(i) + " ");
			}
			System.out.println();
		}
	}

	public static Map<String, String> readCorrectionMap(String fileName)
			throws Throwable {
		Map<String, String> directCorrector = new HashMap<>();

		InputStream fis = new FileInputStream(fileName);
		InputStreamReader isr = new InputStreamReader(fis,
				Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			String[] row = line.split(";");
			if (row.length != 2)
				System.out.println("ALERT!!!!");
			directCorrector.put(Util.removeNonChars(row[0]),
					Util.removeNonChars(row[1]));

		}
		br.close();
		return directCorrector;
	}

	public static void TextCleansing(String directory, String correctionFile)
			throws Throwable {
		Map<String, String> corrector = readCorrectionMap(correctionFile);

		Map<Integer, Long> yearlyAmount = new HashMap<>();
		Map<String, Map<Integer, Long>> DirDivYearAmount = new HashMap<>();
		Map<String, Long> DirDivAmount = new HashMap<>();
		Map<String, Long> DirAmount = new HashMap<>();
		Map<String, Map<Integer, Long>> DirYearAmount = new HashMap<>();

		Map<Integer, Integer> yearlyCount = new HashMap<>();
		Map<String, Map<Integer, Integer>> DirDivYearCount = new HashMap<>();
		Map<String, Integer> DirDivCount = new HashMap<>();
		Map<String, Integer> DirCount = new HashMap<>();
		Map<String, Map<Integer, Integer>> DirYearCount = new HashMap<>();

		List<String> fileNames = Util.listFilesForFolder(directory);
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		PrintWriter fileSummary = new PrintWriter(
				"D:\\projects\\NSFFunding\\Summarization.txt");
		int count = 0;
		for (String file : fileNames) {
			File fXmlFile = new File(file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();
			try {
				String title = doc.getElementsByTagName("AwardTitle").item(0)
						.getTextContent().toLowerCase();
				int awardAmount = Integer
						.parseInt(doc.getElementsByTagName("AwardAmount")
								.item(0).getTextContent());
				if (awardAmount <= 0)
					continue;
				String abstraction = Jsoup
						.parse(doc.getElementsByTagName("AbstractNarration")
								.item(0).getTextContent())
						.text().toLowerCase();
				String startDate = doc.getElementsByTagName("StartDate").item(0)
						.getTextContent();
				int year = Integer.parseInt(startDate.split("/")[2]);
				long dateLong = Util.normalizeDate(startDate);
				String directorate = Jsoup
						.parse(doc.getElementsByTagName("Directorate").item(0)
								.getTextContent())
						.text().toLowerCase();
				directorate = Util.removeNonChars(directorate);
				String correction = null;
				correction = corrector.get(directorate);
				if (correction != null)
					directorate = correction;
				String division = Jsoup
						.parse(doc.getElementsByTagName("Division").item(0)
								.getTextContent())
						.text().toLowerCase();
				division = Util.removeNonChars(division);
				correction = null;
				correction = corrector.get(division);
				if (correction != null)
					division = correction;

				// System.out.println(file);
				// System.out.println("-------" + title + "-----------");
				// System.out.println(awardAmount + " Dollars");

				// Long yearCount = yearlyAmount.get(year);
				// if(yearCount == null)
				// yearlyAmount.put(year, (long) awardAmount);
				// else
				// yearlyAmount.put(year,yearCount+ awardAmount);
				/////////////////////
				Long file1 = yearlyAmount.get(year);
				if (file1 == null)
					yearlyAmount.put(year, (long) awardAmount);
				else
					yearlyAmount.put(year, file1 + awardAmount);
				/////////////////////
				Map<Integer, Long> file2 = DirDivYearAmount
						.get(directorate + " " + division);
				if (file2 == null) {
					Map<Integer, Long> map = new HashMap<>();
					map.put(year, (long) awardAmount);
					DirDivYearAmount.put(directorate + " " + division, map);
				} else {
					Long amount = file2.get(year);
					if (amount == null)
						file2.put(year, (long) awardAmount);
					else
						file2.put(year, amount + awardAmount);
					DirDivYearAmount.put(directorate + " " + division, file2);
				}
				//////////////////////////
				Map<Integer, Long> file3 = DirYearAmount.get(directorate);
				if (file3 == null) {
					Map<Integer, Long> map = new HashMap<>();
					map.put(year, (long) awardAmount);
					DirYearAmount.put(directorate, map);
				} else {
					Long amount = file3.get(year);
					if (amount == null)
						file3.put(year, (long) awardAmount);
					else
						file3.put(year, amount + awardAmount);
					DirYearAmount.put(directorate, file3);
				}
				////////////////////////////
				Long file4 = DirDivAmount.get(directorate + " " + division);
				if (file4 == null)
					DirDivAmount.put(directorate + " " + division,
							(long) awardAmount);
				else
					DirDivAmount.put(directorate + " " + division,
							file4 + awardAmount);
				////////////////////////////
				Long file5 = DirAmount.get(directorate);
				if (file5 == null)
					DirAmount.put(directorate, (long) awardAmount);
				else
					DirAmount.put(directorate, file5 + awardAmount);

				// -------------------------------------------
				/////////////////////
				Integer file12 = yearlyCount.get(year);
				if (file12 == null)
					yearlyCount.put(year, 1);
				else
					yearlyCount.put(year, file12 + 1);
				/////////////////////
				Map<Integer, Integer> file22 = DirDivYearCount
						.get(directorate + " " + division);
				if (file22 == null) {
					Map<Integer, Integer> map = new HashMap<>();
					map.put(year, 1);
					DirDivYearCount.put(directorate + " " + division, map);
				} else {
					Integer amount = file22.get(year);
					if (amount == null)
						file22.put(year, 1);
					else
						file22.put(year, amount + 1);
					DirDivYearCount.put(directorate + " " + division, file22);
				}
				//////////////////////////
				Map<Integer, Integer> file32 = DirYearCount.get(directorate);
				if (file32 == null) {
					Map<Integer, Integer> map = new HashMap<>();
					map.put(year, 1);
					DirYearCount.put(directorate, map);
				} else {
					Integer amount = file32.get(year);
					if (amount == null)
						file32.put(year, 1);
					else
						file32.put(year, amount + 1);
					DirYearCount.put(directorate, file32);
				}
				////////////////////////////
				Integer file42 = DirDivCount.get(directorate + " " + division);
				if (file42 == null)
					DirDivCount.put(directorate + " " + division, 1);
				else
					DirDivCount.put(directorate + " " + division, file42 + 1);
				////////////////////////////
				Integer file52 = DirCount.get(directorate);
				if (file52 == null)
					DirCount.put(directorate, 1);
				else
					DirCount.put(directorate, file52 + 1);

				// PrintWriter outputCleansedText = new PrintWriter(
				// "D:\\projects\\NSFFunding\\CleansedText\\" + count
				// + ".txt");
				// outputCleansedText.print(nlp.cleanseText(title) + " "
				// + nlp.cleanseText(abstraction));
				// outputCleansedText.close();
				// System.out.println(startDate + "->"+dateLong);
				// System.err.println(abstraction);
				count++;
				if (count % 1000 == 0)
					System.out.println("Processed " + count + " tupples");

				fileSummary.println(awardAmount);
			} catch (Exception e) {
				// Do something I'm giving up on you
			}
		}
		fileSummary.close();
		System.out.println(count + "/" + fileNames.size());

		System.out.println("----------------------------------------");
		PrintWriter file1 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 1 + ".txt");
		file1.println("year,amount");
		for (Entry<Integer, Long> entry : yearlyAmount.entrySet()) {
			file1.println(entry.getKey() + "," + entry.getValue());
		}
		file1.close();
		System.out.println("----------------------------------------");
		PrintWriter file2 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 2 + ".txt");
		file2.println("directory-division,year,amount");
		for (Entry<String, Map<Integer, Long>> entry1 : DirDivYearAmount
				.entrySet()) {
			for (Entry<Integer, Long> entry2 : entry1.getValue().entrySet()) {
				file2.println(entry1.getKey() + "," + entry2.getKey() + ","
						+ entry2.getValue());
			}
		}
		System.out.println("----------------------------------------");
		PrintWriter file3 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 3 + ".txt");
		file3.println("directory,year,amount");
		for (Entry<String, Map<Integer, Long>> entry1 : DirYearAmount
				.entrySet()) {
			for (Entry<Integer, Long> entry2 : entry1.getValue().entrySet()) {
				file3.println(entry1.getKey() + "," + entry2.getKey() + ","
						+ entry2.getValue());
			}
		}
		System.out.println("----------------------------------------");
		PrintWriter file4 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 4 + ".txt");
		file4.println("directory-division,amount");
		for (Entry<String, Long> entry : DirDivAmount.entrySet()) {
			file4.println(entry.getKey() + "," + entry.getValue());
		}
		System.out.println("----------------------------------------");
		PrintWriter file5 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 5 + ".txt");
		file5.println("directory,amount");
		for (Entry<String, Long> entry : DirAmount.entrySet()) {
			file5.println(entry.getKey() + "," + entry.getValue());
		}
		System.out.println("----------------------------------------");
		PrintWriter file6 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 6 + ".txt");

		file6.println("year,totalProposal");
		for (Entry<Integer, Integer> entry : yearlyCount.entrySet()) {
			file6.println(entry.getKey() + "," + entry.getValue());
		}
		System.out.println("----------------------------------------");
		PrintWriter file7 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 7 + ".txt");
		file7.println("directory-division,year,totalProposal");
		for (Entry<String, Map<Integer, Integer>> entry1 : DirDivYearCount
				.entrySet()) {
			for (Entry<Integer, Integer> entry2 : entry1.getValue()
					.entrySet()) {
				file7.println(entry1.getKey() + "," + entry2.getKey() + ","
						+ entry2.getValue());
			}
		}
		System.out.println("----------------------------------------");
		PrintWriter file8 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 8 + ".txt");
		file8.println("directory,year,totalProposal");
		for (Entry<String, Map<Integer, Integer>> entry1 : DirYearCount
				.entrySet()) {
			for (Entry<Integer, Integer> entry2 : entry1.getValue()
					.entrySet()) {
				file8.println(entry1.getKey() + "," + entry2.getKey() + ","
						+ entry2.getValue());
			}
		}
		System.out.println("----------------------------------------");
		PrintWriter file9 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 9 + ".txt");
		file9.println("directory-division,totalProposal");
		for (Entry<String, Integer> entry : DirDivCount.entrySet()) {
			file9.println(entry.getKey() + "," + entry.getValue());
		}
		System.out.println("----------------------------------------");
		PrintWriter file10 = new PrintWriter(
				"D:\\projects\\NSFFunding\\file" + 10 + ".txt");
		file10.println("directory,totalProposal");
		for (Entry<String, Integer> entry : DirCount.entrySet()) {
			file10.println(entry.getKey() + "," + entry.getValue());
		}
		file1.close();
		file2.close();
		file3.close();
		file4.close();
		file5.close();
		file6.close();
		file7.close();
		file8.close();
		file9.close();
		file10.close();
	}
}
