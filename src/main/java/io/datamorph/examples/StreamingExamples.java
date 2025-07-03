package io.datamorph.examples;

import io.datamorph.core.DataMorph;
import io.datamorph.core.DataRow;
import io.datamorph.core.DataSource;
import io.datamorph.core.Format;
import io.datamorph.core.StreamDataSource;
import io.datamorph.transform.Transform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 스트리밍 DataSource 사용 예제
 * 
 * @version 1.0.0
 * @since 2025.07.01
 */
public class StreamingExamples {
    
    /**
     * 기본 스트리밍 사용 예제
     */
    public static void basicStreamingExample() {
        System.out.println("=== 기본 스트리밍 예제 ===");
        
        // CSV 데이터 준비
        String csvData = """
            name,age,salary,department
            John,30,50000,Engineering
            Jane,25,45000,Marketing
            Bob,35,60000,Engineering
            Alice,28,52000,HR
            """;
        
        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        
        // 스트리밍 DataSource 생성 및 변환
        List<DataRow> result = DataMorph.fromStream(inputStream, Format.CSV)
            .transform(row -> {
                int salary = row.getInt("salary");
                String grade = salary >= 55000 ? "Senior" : 
                              salary >= 50000 ? "Mid" : "Junior";
                row.set("grade", grade);
            })
            .filter(row -> row.getString("department").equals("Engineering"))
            .toList();
        
        System.out.println("Engineering 부서 직원들:");
        result.forEach(row -> {
            System.out.printf("- %s (%d세, %s등급, 연봉: %d)%n",
                row.getString("name"),
                row.getInt("age"),
                row.getString("grade"),
                row.getInt("salary"));
        });
    }
    
    /**
     * Transform 객체를 사용한 구조화된 변환 예제
     */
    public static void structuredTransformExample() {
        System.out.println("\n=== 구조화된 변환 예제 ===");
        
        String employeeData = """
            emp_name,age,base_salary,department,active
            John Doe,30,50000,Engineering,true
            Jane Smith,25,45000,Marketing,true
            Bob Johnson,35,60000,Engineering,false
            Alice Brown,28,52000,HR,true
            """;
        
        InputStream inputStream = new ByteArrayInputStream(employeeData.getBytes(StandardCharsets.UTF_8));
        
        // Transform 객체 정의
        Transform employeeTransform = Transform.builder()
            .rename("emp_name", "name")
            .rename("base_salary", "salary")
            .add("company", "DataMorph Inc.")
            .add("bonus", 5000)
            .remove("active")
            .build();
        
        // 변환 실행
        List<DataRow> result = DataMorph.fromStream(inputStream, Format.CSV)
            .transform(employeeTransform)
            .transform(row -> {
                // 총 급여 계산
                int totalSalary = row.getInt("salary") + row.getInt("bonus");
                row.set("total_salary", totalSalary);
            })
            .filter(row -> row.getInt("age") >= 28)
            .toList();
        
        System.out.println("28세 이상 직원들의 급여 정보:");
        result.forEach(row -> {
            System.out.printf("- %s: 기본급 %d + 보너스 %d = 총급여 %d%n",
                row.getString("name"),
                row.getInt("salary"),
                row.getInt("bonus"),
                row.getInt("total_salary"));
        });
    }
    
    /**
     * 조건부 변환 예제
     */
    public static void conditionalTransformExample() {
        System.out.println("\n=== 조건부 변환 예제 ===");
        
        String salesData = """
            name,age,sales_amount,region
            Tom,32,75000,North
            Sara,29,65000,South
            Mike,45,85000,North
            Lisa,31,70000,East
            """;
        
        InputStream inputStream = new ByteArrayInputStream(salesData.getBytes(StandardCharsets.UTF_8));
        
        Transform conditionalTransform = Transform.builder()
            .when(
                row -> row.getInt("age") > 40,
                row -> {
                    row.set("seniority", "Senior");
                    return row;
                }
            )
            .when(
                row -> row.getInt("sales_amount") > 70000,
                row -> {
                    row.set("performance", "Excellent");
                    return row;
                }
            )
            .build();
        
        List<DataRow> result = DataMorph.fromStream(inputStream, Format.CSV)
            .transform(conditionalTransform)
            .transform(row -> {
                // 기본값 설정
                if (!row.has("seniority")) {
                    row.set("seniority", "Regular");
                }
                if (!row.has("performance")) {
                    row.set("performance", "Good");
                }
            })
            .toList();
        
        System.out.println("영업직원 평가 결과:");
        result.forEach(row -> {
            System.out.printf("- %s (%s, %s): 매출 %d (%s)%n",
                row.getString("name"),
                row.getString("region"),
                row.getString("seniority"),
                row.getInt("sales_amount"),
                row.getString("performance"));
        });
    }
    
    /**
     * 지연 평가 동작 시연 예제
     */
    public static void lazyEvaluationExample() {
        System.out.println("\n=== 지연 평가 동작 시연 ===");
        
        String data = """
            id,value
            1,100
            2,200
            3,300
            4,400
            5,500
            """;
        
        InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("1. StreamDataSource 생성 중...");
        long start = System.currentTimeMillis();
        
        // 여러 변환을 체이닝하지만 아직 실행되지 않음
        DataSource dataSource = DataMorph.fromStream(inputStream, Format.CSV)
            .transform(row -> {
                System.out.println("  변환 실행: " + row.getString("id"));
                row.set("doubled", row.getInt("value") * 2);
            })
            .filter(row -> {
                System.out.println("  필터링 확인: " + row.getString("id"));
                return row.getInt("value") > 200;
            });
        
        long middle = System.currentTimeMillis();
        System.out.printf("2. 변환 체이닝 완료 (소요시간: %dms)%n", middle - start);
        System.out.println("3. 실제 처리 시작 (toList() 호출)...");
        
        // 이 시점에서 실제 처리가 수행됨
        List<DataRow> result = dataSource.toList();
        
        long end = System.currentTimeMillis();
        System.out.printf("4. 처리 완료 (소요시간: %dms)%n", end - middle);
        System.out.printf("결과: %d개 행%n", result.size());
    }
    
    /**
     * 변환 단계 추적 예제
     */
    public static void transformStepTrackingExample() {
        System.out.println("\n=== 변환 단계 추적 예제 ===");
        
        String data = """
            name,score
            Alice,85
            Bob,92
            Charlie,78
            """;
        
        InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        
        StreamDataSource streamSource = (StreamDataSource) DataMorph.fromStream(inputStream, Format.CSV)
            .transform(row -> row.set("processed", true))
            .filter(row -> row.getInt("score") >= 80)
            .transform(Transform.builder()
                .add("grade", "A")
                .build());
        
        System.out.println("설정된 변환 단계들:");
        List<String> descriptions = streamSource.getTransformStepDescriptions();
        descriptions.forEach(System.out::println);
        
        System.out.printf("총 %d개의 변환 단계가 설정됨%n", streamSource.getTransformStepCount());
        
        // 실제 실행
        List<DataRow> result = streamSource.toList();
        System.out.printf("결과: %d개 행 처리 완료%n", result.size());
    }
    
    /**
     * 모든 예제 실행
     */
    public static void main(String[] args) {
        basicStreamingExample();
        structuredTransformExample();
        conditionalTransformExample();
        lazyEvaluationExample();
        transformStepTrackingExample();
    }
}
