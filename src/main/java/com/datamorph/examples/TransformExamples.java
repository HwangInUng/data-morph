package com.datamorph.examples;

import com.datamorph.core.DataMorph;
import com.datamorph.core.DataRow;
import com.datamorph.core.DataSource;
import com.datamorph.transform.Transform;

import java.util.List;

/**
 * Transform 패키지 사용 예제
 *
 * @version 1.0.0
 * @since 2025.06.30
 */
public class TransformExamples {
    
    public static void main(String[] args) {
        System.out.println("=== DataMorph Transform 예제 ===\n");
        
        // 1. 기본 Transform 연산들
        basicTransformOperations();
        
        // 2. 복합 Transform 체이닝
        chainedTransformOperations();
        
        // 3. 조건부 Transform
        conditionalTransformOperations();
    }
    
    /**
     * 기본 Transform 연산들 예제
     */
    private static void basicTransformOperations() {
        System.out.println("1. 기본 Transform 연산들\n");
        
        // 테스트 CSV 데이터 생성
        String csvData = """
            emp_name,salary,age,department
            John Doe,50000,30,IT
            Jane Smith,60000,55,HR
            Bob Johnson,45000,25,Finance
            """;
        
        DataSource dataSource = DataMorph.fromString(csvData, com.datamorph.core.Format.CSV);
        
        // 1-1. 필드 이름 변경
        System.out.println("1-1. 필드 이름 변경 (emp_name → name):");
        Transform renameTransform = Transform.builder()
                .rename("emp_name", "name")
                .build();
        
        List<DataRow> renamed = dataSource.transform(renameTransform).toList();
        printRows(renamed);
        
        // 1-2. 새 필드 추가
        System.out.println("\n1-2. 새 필드 추가 (bonus=1000, status=active):");
        Transform addTransform = Transform.builder()
                .add("bonus", 1000)
                .add("status", "active")
                .build();
        
        List<DataRow> added = dataSource.transform(addTransform).toList();
        printRows(added);
        
        // 1-3. 필드 제거
        System.out.println("\n1-3. 필드 제거 (department 제거):");
        Transform removeTransform = Transform.builder()
                .remove("department")
                .build();
        
        List<DataRow> removed = dataSource.transform(removeTransform).toList();
        printRows(removed);
    }
    
    /**
     * 복합 Transform 체이닝 예제
     */
    private static void chainedTransformOperations() {
        System.out.println("\n\n2. 복합 Transform 체이닝\n");
        
        String csvData = """
            emp_name,salary,age,department
            John Doe,50000,30,IT
            Jane Smith,60000,55,HR
            Bob Johnson,45000,25,Finance
            """;
        
        DataSource dataSource = DataMorph.fromString(csvData, com.datamorph.core.Format.CSV);
        
        // 여러 연산을 체이닝
        Transform chainedTransform = Transform.builder()
                .rename("emp_name", "name")           // 이름 변경
                .add("bonus", 1000)                   // 보너스 추가
                .remove("department")                 // 부서 정보 제거
                .build();
        
        System.out.println("체이닝된 변환 적용:");
        System.out.println("- emp_name → name");
        System.out.println("- bonus 필드 추가 (1000)");
        System.out.println("- department 필드 제거");
        
        List<DataRow> result = dataSource.transform(chainedTransform).toList();
        printRows(result);
        
        // Transform 설명 출력
        System.out.println("\n적용된 변환 연산들:");
        List<String> descriptions = chainedTransform.getOperationDescriptions();
        for (int i = 0; i < descriptions.size(); i++) {
            System.out.println((i + 1) + ". " + descriptions.get(i));
        }
    }
    
    /**
     * 조건부 Transform 예제
     */
    private static void conditionalTransformOperations() {
        System.out.println("\n\n3. 조건부 Transform\n");
        
        String csvData = """
            emp_name,salary,age,department
            John Doe,50000,30,IT
            Jane Smith,60000,55,HR
            Bob Johnson,45000,25,Finance
            Alice Wilson,70000,52,IT
            """;
        
        DataSource dataSource = DataMorph.fromString(csvData, com.datamorph.core.Format.CSV);
        
        // 조건부 변환: 나이가 50 이상이면 시니어 카테고리 설정
        Transform conditionalTransform = Transform.builder()
                .when(row -> row.getInt("age") > 50, 
                      senior -> {
                          senior.set("category", "senior");
                          senior.set("experience_bonus", 5000);
                          return senior;
                      }, "시니어 직원 처리")
                .when(row -> row.getInt("salary") > 60000,
                      highEarner -> {
                          highEarner.set("tax_bracket", "high");
                          return highEarner;
                      }, "고소득자 처리")
                .build();
        
        System.out.println("조건부 변환 적용:");
        System.out.println("- 나이 50 이상: category='senior', experience_bonus=5000");
        System.out.println("- 급여 60000 이상: tax_bracket='high'");
        
        List<DataRow> result = dataSource.transform(conditionalTransform).toList();
        printRows(result);
        
        // Transform 설명 출력
        System.out.println("\n적용된 변환 연산들:");
        List<String> descriptions = conditionalTransform.getOperationDescriptions();
        for (int i = 0; i < descriptions.size(); i++) {
            System.out.println((i + 1) + ". " + descriptions.get(i));
        }
    }
    
    /**
     * DataRow 리스트를 출력하는 헬퍼 메서드
     */
    private static void printRows(List<DataRow> rows) {
        if (rows.isEmpty()) {
            System.out.println("(데이터 없음)");
            return;
        }
        
        for (DataRow row : rows) {
            System.out.println(row);
        }
    }
}
