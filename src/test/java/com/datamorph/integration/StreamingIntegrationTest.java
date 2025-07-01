package com.datamorph.integration;

import com.datamorph.core.DataMorph;
import com.datamorph.core.DataRow;
import com.datamorph.core.DataSource;
import com.datamorph.core.Format;
import com.datamorph.core.StreamDataSource;
import com.datamorph.transform.Transform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 스트리밍 기능 통합 테스트
 */
class StreamingIntegrationTest {

	@TempDir
	Path tempDir;

	private String csvData;

	@BeforeEach
	void setUp () {
		csvData = """
				emp_name,age,salary,department,active
				John Doe,30,50000,Engineering,true
				Jane Smith,25,45000,Marketing,true
				Bob Johnson,35,60000,Engineering,false
				Alice Brown,28,52000,HR,true
				""";
	}

	@Test
	@DisplayName("CSV 스트림 변환 체이닝 통합 테스트")
	void testCsvStreamTransformChaining () {
		// Given
		InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

		// When
		List<DataRow> result = DataMorph.fromStream(inputStream, Format.CSV)
										.transform(Transform.builder()
															.rename("emp_name", "name")
															.add("bonus", 1000)
															.remove("active")
															.build())
										.filter(row -> row.getString("department").equals("Engineering"))
										.transform(row -> {
											int currentSalary = row.getInt("salary");
											row.set("total_compensation", currentSalary + row.getInt("bonus"));
										})
										.toList();

		// Then
		assertEquals(2, result.size());

		DataRow john = result.stream()
							 .filter(row -> "John Doe".equals(row.getString("name")))
							 .findFirst()
							 .orElseThrow();

		assertEquals("John Doe", john.getString("name"));
		assertEquals(30, john.getInt("age"));
		assertEquals(50000, john.getInt("salary"));
		assertEquals(1000, john.getInt("bonus"));
		assertEquals(51000, john.getInt("total_compensation"));
		assertEquals("Engineering", john.getString("department"));
		assertFalse(john.has("emp_name"));
		assertFalse(john.has("active"));
	}

	@Test
	@DisplayName("지연 평가 동작 검증 테스트")
	void testLazyEvaluationBehavior () {
		// Given
		InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

		// When - 여러 변환을 체이닝하지만 아직 실행하지 않음
		long startTime = System.currentTimeMillis();

		DataSource dataSource = DataMorph.fromStream(inputStream, Format.CSV)
										 .transform(row -> row.set("processed", true))
										 .filter(row -> row.getInt("age") > 25);

		long middleTime = System.currentTimeMillis();

		// Then - 아직 실제 처리는 수행되지 않음 (지연 평가)
		assertTrue(middleTime - startTime < 100);
		assertTrue(dataSource instanceof StreamDataSource);

		// When - 실제 실행
		List<DataRow> result = dataSource.toList();

		// Then - 결과 검증
		assertEquals(3, result.size());
		result.forEach(row -> assertTrue(row.getBoolean("processed")));
	}

	@Test
	@DisplayName("파일 기반 스트리밍 DataSource 테스트")
	void testFileBasedStreamingDataSource () throws IOException {
		// Given
		Path csvFile = tempDir.resolve("employees.csv");
		Files.writeString(csvFile, csvData);

		// When
		List<DataRow> result = DataMorph.fromStreamFile(csvFile.toString())
										.transform(row -> row.set("processed_date", "2025-07-01"))
										.filter(row -> row.getInt("age") >= 28)
										.toList();

		// Then
		assertEquals(3, result.size());
		result.forEach(row -> {
			assertEquals("2025-07-01", row.getString("processed_date"));
			assertTrue(row.getInt("age") >= 28);
		});
	}

	@Test
	@DisplayName("변환 단계 추적 테스트")
	void testTransformStepTracking () {
		// Given
		InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

		// When
		StreamDataSource dataSource = (StreamDataSource) DataMorph.fromStream(inputStream, Format.CSV)
																  .transform(row -> row.set("step1", "done"))
																  .filter(row -> row.getInt("age") > 25)
																  .transform(Transform.builder().add("step2", "done").build());

		// Then
		assertEquals(3, dataSource.getTransformStepCount());

		List<String> descriptions = dataSource.getTransformStepDescriptions();
		assertEquals(3, descriptions.size());

		assertTrue(descriptions.get(0).contains("Step 1"));
		assertTrue(descriptions.get(1).contains("Step 2"));
		assertTrue(descriptions.get(2).contains("Step 3"));
	}

	@Test
	@DisplayName("스트림 자원 관리 테스트")
	void testStreamResourceManagement () throws IOException {
		// Given
		Path testFile = tempDir.resolve("test.csv");
		Files.writeString(testFile, csvData);

		// When
		DataSource dataSource = DataMorph.fromStreamFile(testFile.toString());

		// Then - StreamDataSource가 생성되고 InputStream이 관리되는지 확인
		assertTrue(dataSource instanceof StreamDataSource);
		StreamDataSource streamSource = (StreamDataSource) dataSource;
		assertTrue(streamSource.isInputStreamAvailable());

		List<DataRow> result = streamSource.toList();
		assertEquals(4, result.size());
	}

	@Test
	@DisplayName("메모리 효율성 검증 - 대용량 데이터 시뮬레이션")
	void testMemoryEfficiencyWithLargeDataset () {
		// Given - 대용량 CSV 데이터 시뮬레이션
		StringBuilder largeDataBuilder = new StringBuilder();
		largeDataBuilder.append("id,name,value\n");

		for (int i = 1; i <= 1000; i++) {
			largeDataBuilder.append(i)
							.append(",Employee")
							.append(i)
							.append(",")
							.append(i * 100)
							.append("\n");
		}

		InputStream largeInputStream = new ByteArrayInputStream(
				largeDataBuilder.toString().getBytes(StandardCharsets.UTF_8));

		long startTime = System.currentTimeMillis();

		List<DataRow> result = DataMorph.fromStream(largeInputStream, Format.CSV)
										.filter(row -> row.getInt("id") % 10 == 0) // 10의 배수만 필터링
										.transform(row -> row.set("filtered", true))
										.toList();

		long endTime = System.currentTimeMillis();

		// Then
		assertEquals(100, result.size());
		assertTrue(endTime - startTime < 5000);

		result.forEach(row -> {
			assertEquals(0, row.getInt("id") % 10);
			assertTrue(row.getBoolean("filtered"));
		});
	}
}
