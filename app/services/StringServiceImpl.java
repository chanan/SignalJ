package services;

public class StringServiceImpl implements StringService {

	@Override
	public String capitalize(String sentence) {
		return sentence.toUpperCase();
	}
}