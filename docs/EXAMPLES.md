# DataMorph 사용 예제

이 문서는 DataMorph의 다양한 사용 패턴과 실제 사례를 제공합니다.

## 목차

- [기본 사용법](#기본-사용법)
- [데이터 변환](#데이터-변환)
- [파일 처리](#파일-처리)
- [스트리밍 처리](#스트리밍-처리)
- [고급 사용법](#고급-사용법)
- [실전 사례](#실전-사례)

---

## 기본 사용법

### CSV 파일 읽기와 기본 변환

```java
// 직원 CSV 파일 처리
DataSource employees = DataMorph.from("employees.csv");

List<DataRow> seniorEmployees = employees
    .filter(row -> row.getInt("age") >= 30)
    .filter(row -> "Engineering".equals(row.getString("department")))
    .transform(row -> {
        // 급여 10% 인상
        int salary = row.getInt("salary");
        row.set("salary", (int)(salary * 1.1));
        row.set("bonus_applied", true);
    })
    .toList();
```

### JSON 데이터 처리

```java
String jsonData = """
    [
        {"name": "Alice", "score": 85, "subject": "Math"},
        {"name": "Bob", "score": 92, "subject": "Science"},
        {"name": "Charlie", "score": 78, "subject": "Math"}
    ]
    """;

List<DataRow> topStudents = DataMorph.fromString(jsonData, Format.JSON)
    .filter(row -> row.getInt("score") >= 85)
    .transform(row -> {
        int score = row.getInt("score");
        String grade = score >= 90 ? "A" : "B";
        row.set("grade", grade);
    })
    .toList();
```

### 객체 리스트 처리

```java
List<Employee> employees = Arrays.asList(
    new Employee("John", 30, "Engineering", 50000),
    new Employee("Jane", 25, "Marketing", 45000)
);

// 객체를 DataSource로 변환 후 처리
DataMorph.fromObjects(employees)
    .filter(row -> row.getInt("age") >= 25)
    .transform(row -> row.set("experience_level", "Mid"))
    .toFile("processed_employees.json");
```

---

## 데이터 변환

### Transform 빌더 사용

```java
// 재사용 가능한 변환 규칙 정의
Transform employeeTransform = Transform.builder()
    .rename("emp_name", "name")
    .rename("emp_id", "id")
    .add("company", "DataMorph Inc.")
    .add("bonus", 5000)
    .remove("internal_notes")
    .when(row -> row.getInt("years_exp") > 5, 
          row -> row.set("level", "Senior"))
    .when(row -> row.getInt("years_exp") <= 5, 
          row -> row.set("level", "Junior"))
    .build();

// 여러 데이터소스에 동일한 변환 적용
DataMorph.from("employees_2023.csv")
    .transform(employeeTransform)
    .toFile("standardized_2023.json");

DataMorph.from("employees_2024.csv")
    .transform(employeeTransform)
    .toFile("standardized_2024.json");
```

### 복합 조건 변환

```java
DataMorph.from("sales.csv")
    .transform(row -> {
        double amount = row.getDouble("amount");
        String region = row.getString("region");
        
        // 지역별 할인율 적용
        double discount = switch (region) {
            case "Seoul" -> 0.1;
            case "Busan" -> 0.08;
            case "Daegu" -> 0.05;
            default -> 0.0;
        };
        
        row.set("discount_rate", discount);
        row.set("final_amount", amount * (1 - discount));
        
        // 카테고리 분류
        if (amount > 100000) {
            row.set("category", "Premium");
        } else if (amount > 50000) {
            row.set("category", "Standard");
        } else {
            row.set("category", "Basic");
        }
    })
    .toFile("processed_sales.csv");
```

### 데이터 정제 및 검증

```java
DataMorph.from("raw_data.csv")
    .filter(row -> {
        // 필수 필드 검증
        return row.getString("name") != null && 
               !row.getString("name").trim().isEmpty() &&
               row.getInt("age") != null &&
               row.getInt("age") > 0;
    })
    .transform(row -> {
        // 데이터 정제
        String name = row.getString("name").trim().toUpperCase();
        row.set("name", name);
        
        // 이메일 도메인 추출
        String email = row.getString("email");
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            row.set("email_domain", domain);
        }
        
        // 나이 그룹 설정
        int age = row.getInt("age");
        String ageGroup = age < 20 ? "Teen" :
                         age < 30 ? "20s" :
                         age < 40 ? "30s" :
                         age < 50 ? "40s" : "50+";
        row.set("age_group", ageGroup);
    })
    .toFile("cleaned_data.json");
```

---

## 파일 처리

### 파일 형식 변환

```java
// CSV → JSON 변환
DataMorph.convertFile("input.csv", "output.json");

// JSON → CSV 변환
DataMorph.convertFile("input.json", "output.csv");

// 명시적 형식 지정
DataMorph.convertFile("data.txt", "result.txt", Format.CSV, Format.JSON);
```

### 배치 파일 처리

```java
String[] inputFiles = {"file1.csv", "file2.csv", "file3.csv"};
List<DataRow> allData = new ArrayList<>();

for (String file : inputFiles) {
    List<DataRow> fileData = DataMorph.from(file)
        .filter(row -> "ACTIVE".equals(row.getString("status")))
        .transform(row -> row.set("source_file", file))
        .toList();
    
    allData.addAll(fileData);
}

// 통합된 데이터 저장
DataMorph.fromObjects(allData).toFile("merged_data.json");
```

### 동적 파일명 생성

```java
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

DataMorph.from("daily_report.csv")
    .filter(row -> row.getDouble("revenue") > 1000.0)
    .transform(row -> {
        row.set("processed_at", timestamp);
        row.set("currency", "KRW");
    })
    .toFile("high_revenue_" + timestamp + ".json");
```

---

## 스트리밍 처리

### 대용량 파일 처리

```java
// 대용량 CSV 파일을 스트리밍으로 처리
try (InputStream inputStream = new FileInputStream("large_dataset.csv")) {
    DataMorph.fromStream(inputStream, Format.CSV)
        .filter(row -> {
            // 메모리 효율적인 필터링
            String status = row.getString("status");
            return "ACTIVE".equals(status) || "PENDING".equals(status);
        })
        .transform(row -> {
            // 필요한 필드만 유지
            row.set("processed", true);
            row.set("timestamp", System.currentTimeMillis());
        })
        .toFile("filtered_large_dataset.json");
}
```

### 지연 평가 활용

```java
// 변환 파이프라인 구성 (아직 실행되지 않음)
DataSource pipeline = DataMorph.fromStream(inputStream, Format.CSV)
    .filter(row -> row.getInt("score") > 80)
    .transform(row -> row.set("grade", "Pass"))
    .filter(row -> "Engineering".equals(row.getString("department")));

// 필요한 시점에 실행
if (shouldProcess) {
    List<DataRow> results = pipeline.toList();
    // 결과 처리
}
```

### 메모리 모니터링

```java
StreamDataSource streamSource = (StreamDataSource) DataMorph.fromStream(inputStream, Format.CSV)
    .transform(row -> row.set("processed", true))
    .filter(row -> row.getInt("amount") > 1000);

// 변환 단계 확인
System.out.println("변환 단계 수: " + streamSource.getTransformStepCount());

List<String> descriptions = streamSource.getTransformStepDescriptions();
descriptions.forEach(System.out::println);

// 실제 처리 실행
List<DataRow> results = streamSource.toList();
```

---

## 고급 사용법

### 에러 처리 및 복구

```java
public void processDataWithErrorHandling(String filePath) {
    try {
        DataSource data = DataMorph.from(filePath);
        
        List<DataRow> results = data
            .filter(row -> {
                try {
                    // 안전한 정수 변환
                    Integer age = row.getInt("age");
                    return age != null && age > 0 && age < 150;
                } catch (NumberFormatException e) {
                    logger.warn("Invalid age value in row: {}", row);
                    return false; // 잘못된 데이터는 제외
                }
            })
            .transform(row -> {
                try {
                    // 안전한 데이터 변환
                    String email = row.getString("email");
                    if (email != null && email.contains("@")) {
                        row.set("email_valid", true);
                        row.set("email_domain", email.split("@")[1]);
                    } else {
                        row.set("email_valid", false);
                        row.set("email_domain", "unknown");
                    }
                } catch (Exception e) {
                    logger.warn("Error processing email for row: {}", row, e);
                    row.set("email_valid", false);
                    row.set("email_domain", "error");
                }
            })
            .toList();
            
        logger.info("Successfully processed {} rows", results.size());
        
    } catch (IllegalArgumentException e) {
        logger.error("File not found or invalid path: {}", filePath, e);
        throw new ProcessingException("Cannot read file: " + filePath, e);
    } catch (ParseException e) {
        logger.error("Failed to parse file: {}", filePath, e);
        throw new ProcessingException("Invalid file format: " + filePath, e);
    } catch (Exception e) {
        logger.error("Unexpected error processing file: {}", filePath, e);
        throw new ProcessingException("Processing failed: " + filePath, e);
    }
}
```

이 예제들을 통해 DataMorph의 다양한 활용 방법을 확인할 수 있습니다.