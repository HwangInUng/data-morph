package com.datamorph.examples;

import com.datamorph.core.DataMorph;
import com.datamorph.core.DataSource;
import com.datamorph.core.Format;
import com.datamorph.exceptions.ParseException;

import java.util.Arrays;
import java.util.List;

/**
 * DataMorph Writer 시스템 사용 예제
 * <p>
 * 이 클래스는 Writer 시스템의 다양한 기능과 사용법을 보여줍니다.
 * </p>
 *
 * @version 1.0.0
 * @since 2025.07.02
 */
public class WriterExamples {

	// 상수 정의
	private static final String EMPLOYEES_CSV = "employees.csv";
	private static final String DEPARTMENT_FIELD = "department";
	private static final String SALARY_FIELD = "salary";

	// 예제용 데이터 클래스
	static class Employee {
		private String name;
		private int age;
		private String department;
		private double salary;
		private boolean active;

		public Employee () {}

		public Employee (String name, int age, String department, double salary, boolean active) {
			this.name = name;
			this.age = age;
			this.department = department;
			this.salary = salary;
			this.active = active;
		}

		// Getters
		public String getName () {
			return name;
		}

		public int getAge () {
			return age;
		}

		public String getDepartment () {
			return department;
		}

		public double getSalary () {
			return salary;
		}

		public boolean isActive () {
			return active;
		}

		// Setters
		public void setName (String name) {
			this.name = name;
		}

		public void setAge (int age) {
			this.age = age;
		}

		public void setDepartment (String department) {
			this.department = department;
		}

		public void setSalary (double salary) {
			this.salary = salary;
		}

		public void setActive (boolean active) {
			this.active = active;
		}

		@Override
		public String toString () {
			return "name :: " + name + ", age :: " + age + ", department :: " + department + ", salary :: " + salary + ", active :: " + active;
		}
	}

	public static void main (String[] args) {
		System.out.println("=== DataMorph Writer 시스템 예제 ===\n");

		try {
			// 예제 데이터 준비
			List<Employee> employees = prepareEmployeeData();

			// 1. 객체 리스트를 파일로 저장
			basicFileWritingExamples(employees);

			// 2. 데이터 변환 및 저장
			dataTransformationExamples();

			// 3. 파일 포맷 변환
			fileFormatConversionExamples();

			// 4. 문자열 변환
			stringConversionExamples(employees);

			// 5. 고급 활용
			advancedUsageExamples(employees);

		} catch (Exception e) {
			System.err.println("예제 실행 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 예제 데이터 준비
	 */
	private static List<Employee> prepareEmployeeData () {
		return Arrays.asList(
				new Employee("김철수", 30, "개발팀", 5500000, true),
				new Employee("이영희", 28, "디자인팀", 4800000, true),
				new Employee("박민수", 35, "개발팀", 7000000, false),
				new Employee("정수진", 32, "마케팅팀", 5200000, true),
				new Employee("홍길동", 29, "개발팀", 5800000, true)
		);
	}

	/**
	 * 1. 기본 파일 쓰기 예제
	 */
	private static void basicFileWritingExamples (List<Employee> employees) {
		System.out.println("1. 기본 파일 쓰기 예제");
		System.out.println("=".repeat(50));

		// 객체 리스트를 CSV 파일로 저장
		System.out.println("객체 리스트를 CSV 파일로 저장...");
		DataMorph.saveObjectsToFile(employees, EMPLOYEES_CSV);
		System.out.println("✓ employees.csv 파일이 생성되었습니다.");

		// 객체 리스트를 JSON 파일로 저장
		System.out.println("객체 리스트를 JSON 파일로 저장...");
		DataMorph.saveObjectsToFile(employees, "employees.json", Format.JSON);
		System.out.println("✓ employees.json 파일이 생성되었습니다.");

		// DataSource를 사용한 파일 저장
		System.out.println("DataSource를 사용한 파일 저장...");
		DataSource dataSource = DataMorph.fromObjects(employees);
		dataSource.toFile("employees_via_datasource.csv");
		System.out.println("✓ employees_via_datasource.csv 파일이 생성되었습니다.");

		System.out.println();
	}

	/**
	 * 2. 데이터 변환 및 저장 예제
	 */
	private static void dataTransformationExamples () throws ParseException {
		System.out.println("2. 데이터 변환 및 저장 예제");
		System.out.println("=".repeat(50));

		// CSV 파일 읽기 → 변환 → 새 파일로 저장
		System.out.println("CSV 파일을 읽어서 변환 후 저장...");
		DataSource originalData = DataMorph.from(EMPLOYEES_CSV);

		// 개발팀 직원만 필터링하고 급여 10% 인상 후 저장
		originalData
				.filter(row -> "개발팀".equals(row.getString(DEPARTMENT_FIELD)))
				.transform(row -> {
					double currentSalary = row.getDouble(SALARY_FIELD);
					row.set(SALARY_FIELD, currentSalary * 1.1); // 10% 인상
					row.set("updated", true); // 업데이트 표시 추가
				})
				.toFile("dev_team_raised.json");

		System.out.println("✓ dev_team_raised.json 파일이 생성되었습니다.");
		System.out.println("  (개발팀 직원만 필터링, 급여 10% 인상)");

		System.out.println();
	}

	/**
	 * 3. 파일 포맷 변환 예제
	 */
	private static void fileFormatConversionExamples () {
		System.out.println("3. 파일 포맷 변환 예제");
		System.out.println("=".repeat(50));

		// CSV → JSON 변환
		System.out.println("CSV 파일을 JSON으로 변환...");
		DataMorph.convertFile(EMPLOYEES_CSV, "employees_converted.json");
		System.out.println("✓ employees.csv → employees_converted.json");

		// JSON → CSV 변환
		System.out.println("JSON 파일을 CSV로 변환...");
		DataMorph.convertFile("employees.json", "employees_from_json.csv");
		System.out.println("✓ employees.json → employees_from_json.csv");

		// 포맷을 명시적으로 지정한 변환
		System.out.println("포맷을 명시적으로 지정한 변환...");
		DataMorph.convertFile(
				EMPLOYEES_CSV,
				"employees_explicit.txt",
				Format.CSV,
				Format.JSON
		);
		System.out.println("✓ employees.csv → employees_explicit.txt (JSON 포맷)");

		System.out.println();
	}

	/**
	 * 4. 문자열 변환 예제
	 */
	private static void stringConversionExamples (List<Employee> employees) {
		System.out.println("4. 문자열 변환 예제");
		System.out.println("=".repeat(50));

		DataSource dataSource = DataMorph.fromObjects(employees);

		// CSV 문자열로 변환
		System.out.println("데이터를 CSV 문자열로 변환:");
		String csvString = dataSource.toString(Format.CSV);
		System.out.println(csvString.substring(0, Math.min(csvString.length(), 200)) + "...");

		System.out.println("\n데이터를 JSON 문자열로 변환:");
		String jsonString = dataSource.toString(Format.JSON);
		System.out.println(jsonString.substring(0, Math.min(jsonString.length(), 300)) + "...");

		// 문자열 데이터를 파일로 저장
		System.out.println("\n문자열 데이터를 파일로 저장...");
		String csvData = "name,position,experience\n홍길동,시니어개발자,5\n김영수,주니어개발자,2";
		DataMorph.saveStringToFile(csvData, Format.CSV, "new_employees.json");
		System.out.println("✓ CSV 문자열 데이터를 JSON 파일로 저장했습니다.");

		System.out.println();
	}

	/**
	 * 5. 고급 활용 예제
	 */
	private static void advancedUsageExamples (List<Employee> employees) throws ParseException {
		System.out.println("5. 고급 활용 예제");
		System.out.println("=".repeat(50));

		// 연쇄 변환 파이프라인
		System.out.println("연쇄 변환 파이프라인 실행...");
		DataSource pipeline = DataMorph.fromObjects(employees)
									   .filter(row -> row.getInt("age") >= 30) // 30세 이상
									   .transform(row -> {
										   // 부서명을 영어로 변환
										   String dept = row.getString(DEPARTMENT_FIELD);
										   String englishDept = switch (dept) {
											   case "개발팀" -> "Development";
											   case "디자인팀" -> "Design";
											   case "마케팅팀" -> "Marketing";
											   default -> dept;
										   };
										   row.set(DEPARTMENT_FIELD, englishDept);

										   // 급여를 달러로 변환 (1달러 = 1300원 가정)
										   double krwSalary = row.getDouble(SALARY_FIELD);
										   row.set("salary_usd", Math.round(krwSalary / 1300.0));

										   // 시니어 여부 추가
										   row.set("is_senior", row.getInt("age") >= 35);
									   });

		// 결과를 여러 포맷으로 저장
		pipeline.toFile("senior_employees.csv");
		pipeline.toFile("senior_employees.json");

		System.out.println("✓ 30세 이상 직원 데이터를 변환하여 저장했습니다.");
		System.out.println("  - 부서명 영어 변환");
		System.out.println("  - 급여 달러 변환");
		System.out.println("  - 시니어 여부 추가");

		// 통계 정보 생성 및 저장
		System.out.println("\n통계 정보 생성 및 저장...");

		DataSource originalData = DataMorph.fromObjects(employees);
		try {
			List<Employee> employeeList = originalData.toList(Employee.class);

			// 부서별 통계 계산 (간단한 예제)
			double avgSalary = employeeList.stream()
										   .mapToDouble(Employee::getSalary)
										   .average()
										   .orElse(0.0);

			long activeCount = employeeList.stream()
										   .filter(Employee::isActive)
										   .count();

			// 통계를 CSV로 저장
			String statsData = String.format(
					"metric,value%naverage_salary,%.0f%nactive_employees,%d%ntotal_employees,%d",
					avgSalary, activeCount, employeeList.size()
			);

			DataMorph.saveStringToFile(statsData, Format.CSV, "employee_stats.json");
			System.out.println("✓ 직원 통계 정보가 employee_stats.json에 저장되었습니다.");

		} catch (Exception e) {
			System.err.println("통계 정보 생성 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}

		// 백업 및 아카이브
		System.out.println("\n백업 및 아카이브...");
		try {
			// backup 디렉토리가 없으면 생성되도록 상대 경로 사용
			String backupFileName = "employees_backup_" + System.currentTimeMillis() + ".csv";
			originalData.toFile(backupFileName);
			System.out.println("✓ 백업 파일(" + backupFileName + ")이 생성되었습니다.");
		} catch (Exception e) {
			System.err.println("백업 생성 중 오류 발생: " + e.getMessage());
		}

		System.out.println();
	}

	/**
	 * 사용법 안내 출력
	 */
	public static void printUsageGuide () {
		System.out.println("=== DataMorph Writer 시스템 사용법 ===\n");

		System.out.println("1. 객체 리스트를 파일로 저장:");
		System.out.println("   DataMorph.saveObjectsToFile(objects, \"output.csv\");");
		System.out.println("   DataMorph.saveObjectsToFile(objects, \"output.json\", Format.JSON);\n");

		System.out.println("2. DataSource를 파일로 저장:");
		System.out.println("   dataSource.toFile(\"output.csv\");");
		System.out.println("   dataSource.toFile(\"output.txt\", Format.JSON);\n");

		System.out.println("3. 파일 포맷 변환:");
		System.out.println("   DataMorph.convertFile(\"input.csv\", \"output.json\");");
		System.out.println("   DataMorph.convertFile(\"in.txt\", \"out.txt\", Format.CSV, Format.JSON);\n");

		System.out.println("4. 문자열 변환:");
		System.out.println("   String csv = dataSource.toString(Format.CSV);");
		System.out.println("   String json = dataSource.toString(Format.JSON);\n");

		System.out.println("5. 데이터 변환 파이프라인:");
		System.out.println("   DataMorph.fromObjects(objects)");
		System.out.println("       .filter(row -> row.getInt(\"age\") > 25)");
		System.out.println("       .transform(row -> row.set(\"bonus\", 1000))");
		System.out.println("       .toFile(\"result.csv\");\n");

		System.out.println("지원하는 포맷: CSV, JSON");
		System.out.println("자동 포맷 감지: 파일 확장자 기반 (.csv, .json)");
	}
}
