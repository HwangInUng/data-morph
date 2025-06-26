package com.datamorph.fixtures;

public class CsvFixtures {
	public static String basicCsv () {
		return "name,age,city\nJohn,30,Seoul";
	}

	public static String multiRowCsv () {
		return """
				name,age,city
				John,30,Seoul
				Jane,25,Busan
				""".trim();
	}

	public static String csvWithEmptyFields () {
		return "name,age,city\nJohn,,Seoul";
	}

	public static String emptyCsv () {
		return "name,age,city";
	}

	public static String basicCsvWithTwoRows () {
		return "name,age,city\nJohn,30,Seoul\nJane,25,Busan";
	}

	public static String complexQuotedCsv () {
		return "name,description,notes\n\"John \"\"JD\"\" Doe\",\"Software Engineer\nWorks at \"\"Big Tech\"\"\",\"Uses Java, Python, and \"\"other\"\" languages\"";
	}
}
