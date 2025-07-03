package io.datamorph.examples;

import io.datamorph.core.DataMorph;
import io.datamorph.core.DataSource;
import io.datamorph.core.DataRow;
import io.datamorph.core.Format;

import java.util.List;

/**
 * DataMorph 라이브러리 사용 예제들
 * 
 * 이 클래스는 DataMorph의 주요 기능들을 어떻게 사용하는지 보여줍니다.
 * 
 * @version 1.0.0
 * @since 2025.06.25
 */
public class DataMorphExamples {
    
    /**
     * CSV 파일을 읽고 변환하는 예제
     */
    public static void csvFileExample() {
        // CSV 파일에서 데이터 읽기
        DataSource dataSource = DataMorph.from("employees.csv");
        
        // 나이가 30 이상인 직원만 필터링
        DataSource filteredData = dataSource.filter(row -> {
            Integer age = row.getInt("age");
            return age != null && age >= 30;
        });
        
        // 급여에 10% 보너스 추가
        DataSource transformedData = filteredData.transform(row -> {
            Integer salary = row.getInt("salary");
            if (salary != null) {
                int bonusSalary = (int) (salary * 1.1);
                row.set("salary", bonusSalary);
                row.set("bonus_applied", true);
            }
        });
        
        // 결과 출력
        List<DataRow> results = transformedData.toList();
        System.out.println("보너스가 적용된 30세 이상 직원들:");
        for (DataRow row : results) {
            System.out.printf("이름: %s, 나이: %d, 급여: %d%n", 
                row.getString("name"), 
                row.getInt("age"), 
                row.getInt("salary"));
        }
    }
    
    /**
     * JSON 데이터를 문자열에서 읽고 변환하는 예제
     */
    public static void jsonStringExample() {
        String jsonData = """
            [
                {"name": "Alice", "department": "Engineering", "experience": 5},
                {"name": "Bob", "department": "Marketing", "experience": 3},
                {"name": "Charlie", "department": "Engineering", "experience": 7}
            ]
            """;
        
        // JSON 문자열에서 데이터 읽기
        DataSource dataSource = DataMorph.fromString(jsonData, Format.JSON);
        
        // Engineering 부서 직원만 필터링
        DataSource engineeringData = dataSource.filter(row -> 
            "Engineering".equals(row.getString("department"))
        );
        
        // 경력에 따른 레벨 추가
        DataSource leveledData = engineeringData.transform(row -> {
            Integer experience = row.getInt("experience");
            if (experience != null) {
                String level = experience >= 5 ? "Senior" : "Junior";
                row.set("level", level);
            }
        });
        
        // 결과 출력
        List<DataRow> results = leveledData.toList();
        System.out.println("Engineering 부서 직원들:");
        for (DataRow row : results) {
            System.out.printf("이름: %s, 경력: %d년, 레벨: %s%n",
                row.getString("name"),
                row.getInt("experience"),
                row.getString("level"));
        }
    }
    
    /**
     * 체인 방식으로 여러 변환을 연결하는 예제
     */
    public static void chainedTransformationExample() {
        String csvData = """
            name,score,subject
            Alice,85,Math
            Bob,92,Math
            Charlie,78,Math
            Alice,90,Science
            Bob,88,Science
            Charlie,95,Science
            """;
        
        DataSource results = DataMorph.fromString(csvData, Format.CSV)
            // 점수가 85점 이상인 학생만 필터링
            .filter(row -> {
                Integer score = row.getInt("score");
                return score != null && score >= 85;
            })
            // 등급 추가
            .transform(row -> {
                Integer score = row.getInt("score");
                if (score != null) {
                    String grade = score >= 90 ? "A" : "B";
                    row.set("grade", grade);
                }
            })
            // 과목별로 우수 학생 표시
            .transform(row -> {
                String subject = row.getString("subject");
                String grade = row.getString("grade");
                if ("A".equals(grade)) {
                    row.set("excellence", subject + " 우수학생");
                }
            });
        
        // 결과 출력
        System.out.println("우수 학생들 (85점 이상):");
        for (DataRow row : results.toList()) {
            String excellence = row.getString("excellence");
            System.out.printf("이름: %s, 과목: %s, 점수: %d, 등급: %s%s%n",
                row.getString("name"),
                row.getString("subject"),
                row.getInt("score"),
                row.getString("grade"),
                excellence != null ? ", " + excellence : "");
        }
    }
    
    /**
     * 에러 처리 예제
     */
    public static void errorHandlingExample() {
        try {
            // 존재하지 않는 파일 시도
            DataSource dataSource = DataMorph.from("non_existent_file.csv");
        } catch (IllegalArgumentException e) {
            System.err.println("파일 오류: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("파싱 오류: " + e.getMessage());
        }
        
        try {
            // 잘못된 JSON 형식 시도
            String invalidJson = "{ invalid json }";
            DataSource dataSource = DataMorph.fromString(invalidJson, Format.JSON);
        } catch (RuntimeException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        }
    }
    
    /**
     * 타입 안전성 예제
     */
    public static void typeSafetyExample() {
        String csvData = """
            name,age,active,score
            John,30,true,95.5
            Jane,25,false,87.2
            Bob,35,true,92.0
            """;
        
        DataSource dataSource = DataMorph.fromString(csvData, Format.CSV);
        
        List<DataRow> results = dataSource.toList();
        
        System.out.println("타입별 데이터 접근:");
        for (DataRow row : results) {
            String name = row.getString("name");        // 문자열 접근
            Integer age = row.getInt("age");            // 정수 접근
            Boolean active = row.getBoolean("active");  // 불린 접근
            
            System.out.printf("이름: %s (문자열), 나이: %d (정수), 활성: %b (불린)%n",
                name, age, active);
        }
    }
    
    /**
     * 예제 실행 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== DataMorph 사용 예제 ===\n");

        System.out.println("0. CSV 예제:");
        csvFileExample();
        
        System.out.println("1. JSON 문자열 예제:");
        jsonStringExample();
        
        System.out.println("\n2. 체인 변환 예제:");
        chainedTransformationExample();
        
        System.out.println("\n3. 타입 안전성 예제:");
        typeSafetyExample();
        
        System.out.println("\n4. 에러 처리 예제:");
        errorHandlingExample();
        
        System.out.println("\n=== 예제 완료 ===");
    }
}
