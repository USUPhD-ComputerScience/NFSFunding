package Models;

import java.util.HashMap;
import java.util.Map;

public class Word {
	private String mWord = null;
	int mTotalCount = 0;
	private Map<Integer, Integer> yearCount = new HashMap<>();
	private Map<Integer, Long> yearAmount = new HashMap<>();

	public Word(String word, int year, long money) {
		mWord = word.intern();
		yearCount.put(year, 1);
		mTotalCount++;
		yearAmount.put(year, money);
		// TODO Auto-generated constructor stub
	}

	public String toString(){
		return mWord;
	}
	public void addCount(int year) {
		Integer count = yearCount.get(year);
		if (count == null)
			yearCount.put(year, 1);
		else
			yearCount.put(year, count + 1);
		mTotalCount++;
	}
	public int getTotalCount(){
		return mTotalCount;
	}

	public void addMoney(int year, long money) {
		Long amount = yearAmount.get(year);
		if (amount == null)
			yearAmount.put(year, money);
		else
			yearAmount.put(year, amount + money);
	}
	public long getMoneyAmount(int year){

		Long amount = yearAmount.get(year);
		if(amount == null)
			return 0l;
		return amount;
	}
	public int getCount(int year){

		Integer count = yearCount.get(year);
		if(count == null)
			return 0;
		return count;
	}
}
