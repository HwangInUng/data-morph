package com.datamorph.integration;

import com.datamorph.core.DataMorph;
import com.datamorph.core.DataSource;
import com.datamorph.core.DataRow;
import com.datamorph.core.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * DataMorph 전체 통합 테스트
 * 실제 사용 시나리오를 검증합니다.
 */
class DataMorphIntegrationTest {

	@Test
	@DisplayName("CSV 파일 전체 처리 시나리오")
	void csvFileFullProcessingScenario (@TempDir Path tempDir) throws Exception {
		// given: 실제 CSV 파일 생성
		Path csvFile = tempDir.resolve("employees.csv");
		String csvContent = """
				name,age,department,salary,active
				John,30,Engineering,50000,true
				Jane,25,Marketing,45000,true
				Bob,35,Engineering,60000,false
				Alice,28,HR,48000,true
				Charlie,40,Engineering,70000,true
				""";
		Files.writeString(csvFile, csvContent);

		// when: DataMorph로 복합 처리
		List<DataRow> results = DataMorph.from(csvFile.toString())
										 // 1단계: 활성 직원만 필터링
										 .filter(row -> {
											 Boolean active = row.getBoolean("active");
											 return active != null && active;
										 })
										 // 2단계: Engineering 부서 직원만 선택
										 .filter(row -> "Engineering".equals(row.getString("department")))
										 // 3단계: 30세 이상인 직원만
										 .filter(row -> {
											 Integer age = row.getInt("age");
											 return age != null && age >= 30;
										 })
										 // 4단계: 급여에 10% 보너스 적용
										 .transform(row -> {
											 Integer salary = row.getInt("salary");
											 if (salary != null) {
												 row.set("salary", (int) (salary * 1.1));
												 row.set("bonus_applied", true);
											 }
										 })
										 // 5단계: 시니어 레벨 추가
										 .transform(row -> {
											 Integer age = row.getInt("age");
											 if (age != null) {
												 String level = age >= 35 ? "Senior" : "Mid-level";
												 row.set("level", level);
											 }
										 })
										 .toList();

		// then: 결과 검증
		assertThat(results).hasSize(2); // John과 Charlie만 남아야 함

		// John 검증 (30세, Engineering, 활성, 보너스 적용됨)
		DataRow john = results.stream()
							  .filter(row -> "John".equals(row.getString("name")))
							  .findFirst()
							  .orElseThrow();

		assertThat(john.getString("name")).isEqualTo("John");
		assertThat(john.getInt("age")).isEqualTo(30);
		assertThat(john.getString("department")).isEqualTo("Engineering");
		assertThat(john.getInt("salary")).isEqualTo(55000); // 50000 * 1.1
		assertThat(john.getBoolean("bonus_applied")).isTrue();
		assertThat(john.getString("level")).isEqualTo("Mid-level");

		// Charlie 검증 (40세, Engineering, 활성, 보너스 적용됨)
		DataRow charlie = results.stream()
								 .filter(row -> "Charlie".equals(row.getString("name")))
								 .findFirst()
								 .orElseThrow();

		assertThat(charlie.getString("name")).isEqualTo("Charlie");
		assertThat(charlie.getInt("age")).isEqualTo(40);
		assertThat(charlie.getString("department")).isEqualTo("Engineering");
		assertThat(charlie.getInt("salary")).isEqualTo(77000); // 70000 * 1.1
		assertThat(charlie.getBoolean("bonus_applied")).isTrue();
		assertThat(charlie.getString("level")).isEqualTo("Senior");
	}

	@Test
	@DisplayName("JSON 문자열 전체 처리 시나리오")
	void jsonStringFullProcessingScenario () {
		// given: JSON 데이터
		String jsonContent = """
				[
				    {"name": "Alice", "score": 85, "subject": "Math", "grade": 12},
				    {"name": "Bob", "score": 92, "subject": "Science", "grade": 11},
				    {"name": "Charlie", "score": 78, "subject": "Math", "grade": 12},
				    {"name": "David", "score": 95, "subject": "Science", "grade": 12},
				    {"name": "Eve", "score": 88, "subject": "Math", "grade": 11}
				]
				""";

		// when: 복합 처리
		List<DataRow> results = DataMorph.fromString(jsonContent, Format.JSON)
										 // 12학년 학생만
										 .filter(row -> {
											 Integer grade = row.getInt("grade");
											 return grade != null && grade == 12;
										 })
										 // 85점 이상만
										 .filter(row -> {
											 Integer score = row.getInt("score");
											 return score != null && score >= 85;
										 })
										 // 성적 등급 추가
										 .transform(row -> {
											 Integer score = row.getInt("score");
											 if (score != null) {
												 String letterGrade = score >= 90 ? "A" : "B";
												 row.set("letter_grade", letterGrade);
											 }
										 })
										 // 우수학생 표시 추가
										 .transform(row -> {
											 String letterGrade = row.getString("letter_grade");
											 if ("A".equals(letterGrade)) {
												 row.set("honor_student", true);
												 row.set("remarks", "Outstanding Performance");
											 }
										 })
										 .toList();

		// then: 결과 검증
		assertThat(results).hasSize(2); // Alice(85점, B), David(95점, A)

		// Alice 검증
		DataRow alice = results.stream()
							   .filter(row -> "Alice".equals(row.getString("name")))
							   .findFirst()
							   .orElseThrow();

		assertThat(alice.getInt("score")).isEqualTo(85);
		assertThat(alice.getString("letter_grade")).isEqualTo("B");
		assertThat(alice.has("honor_student")).isFalse(); // A등급이 아니므로 추가되지 않음

		// David 검증
		DataRow david = results.stream()
							   .filter(row -> "David".equals(row.getString("name")))
							   .findFirst()
							   .orElseThrow();

		assertThat(david.getInt("score")).isEqualTo(95);
		assertThat(david.getString("letter_grade")).isEqualTo("A");
		assertThat(david.getBoolean("honor_student")).isTrue();
		assertThat(david.getString("remarks")).isEqualTo("Outstanding Performance");
	}

	@Test
	@DisplayName("에러 시나리오 통합 테스트")
	void errorScenarioIntegrationTest (@TempDir Path tempDir) throws Exception {
		// 빈 CSV 파일 처리
		Path emptyCsvFile = tempDir.resolve("empty.csv");
		Files.writeString(emptyCsvFile, "");

		DataSource emptyData = DataMorph.from(emptyCsvFile.toString());
		assertThat(emptyData.toList()).isEmpty();

		// 헤더만 있는 CSV 파일
		Path headerOnlyCsv = tempDir.resolve("header_only.csv");
		Files.writeString(headerOnlyCsv, "name,age,department");

		DataSource headerOnlyData = DataMorph.from(headerOnlyCsv.toString());
		assertThat(headerOnlyData.toList()).isEmpty();

		// 잘못된 JSON 형식
		assertThatThrownBy(() -> {
			DataMorph.fromString("{ invalid json }", Format.JSON);
		}).isInstanceOf(RuntimeException.class)
		  .hasMessageContaining("Invalid JSON format");

		// 존재하지 않는 필드 접근 (null 반환되어야 함)
		String validCsv = "name,age\nJohn,30";
		DataSource data = DataMorph.fromString(validCsv, Format.CSV);
		DataRow row = data.toList().get(0);

		assertThat(row.getString("nonexistent")).isNull();
		assertThat(row.getInt("nonexistent")).isNull();
		assertThat(row.getBoolean("nonexistent")).isNull();
		assertThat(row.has("nonexistent")).isFalse();
	}
}
