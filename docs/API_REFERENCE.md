# DataMorph API Reference

이 문서는 DataMorph 라이브러리의 모든 API를 상세히 설명합니다.

## 목차

- [Core Classes](#core-classes)
- [DataMorph](#datamorph)
- [DataSource](#datasource)
- [Transform](#transform)
- [Utilities](#utilities)

---

## Core Classes

### DataMorph

메인 진입점 클래스입니다.

#### Static Methods

##### from(String filePath)
```java
public static DataSource from(String filePath)
```
파일에서 DataSource를 생성합니다.

**Parameters:**
- `filePath` - 읽을 파일의 경로

**Returns:** DataSource 인스턴스

**Example:**
```java
DataSource data = DataMorph.from("employees.csv");
```

##### fromString(String content, Format format)
```java
public static DataSource fromString(String content, Format format)
```
문자열 콘텐츠에서 DataSource를 생성합니다.

**Parameters:**
- `content` - 파싱할 문자열 데이터
- `format` - 데이터 형식 (CSV, JSON)

**Returns:** DataSource 인스턴스

**Example:**
```java
String csvData = "name,age\nJohn,30";
DataSource data = DataMorph.fromString(csvData, Format.CSV);
```

##### fromObjects(List<?> objects)
```java
public static DataSource fromObjects(List<?> objects)
```
객체 리스트에서 DataSource를 생성합니다.

**Parameters:**
- `objects` - 변환할 객체들의 리스트

**Returns:** DataSource 인스턴스

##### fromStream(InputStream stream, Format format)
```java
public static DataSource fromStream(InputStream stream, Format format)
```
InputStream에서 DataSource를 생성합니다.

**Parameters:**
- `stream` - 데이터를 읽을 InputStream
- `format` - 데이터 형식

**Returns:** StreamDataSource 인스턴스

#### Utility Methods

##### convertFile(String inputPath, String outputPath)
```java
public static void convertFile(String inputPath, String outputPath)
```
파일 형식을 변환합니다.

##### saveObjectsToFile(List<?> objects, String filePath)
```java
public static void saveObjectsToFile(List<?> objects, String filePath)
```
객체 리스트를 파일로 저장합니다.

---

### DataSource

데이터 변환 파이프라인의 핵심 인터페이스입니다.

#### Transformation Methods

##### filter(Predicate<DataRow> predicate)
```java
DataSource filter(Predicate<DataRow> predicate)
```
조건에 맞는 행만 필터링합니다.

**Parameters:**
- `predicate` - 필터링 조건

**Returns:** 새로운 DataSource 인스턴스

**Example:**
```java
dataSource.filter(row -> row.getInt("age") >= 30)
```

##### transform(Function<DataRow, DataRow> transformer)
```java
DataSource transform(Function<DataRow, DataRow> transformer)
```
각 행을 변환합니다.

**Parameters:**
- `transformer` - 변환 함수

**Returns:** 새로운 DataSource 인스턴스

##### transform(Transform transform)
```java
DataSource transform(Transform transform)
```
Transform 객체를 사용하여 변환합니다.

#### Output Methods

##### toList()
```java
List<DataRow> toList()
```
모든 데이터를 List로 수집합니다.

##### toList(Class<T> clazz)
```java
<T> List<T> toList(Class<T> clazz)
```
지정된 클래스 타입의 객체 리스트로 변환합니다.

##### toFile(String filePath)
```java
void toFile(String filePath)
```
결과를 파일로 저장합니다.

##### toFile(String filePath, Format format)
```java
void toFile(String filePath, Format format)
```
지정된 형식으로 파일에 저장합니다.

##### toString(Format format)
```java
String toString(Format format)
```
지정된 형식의 문자열로 변환합니다.

---

### Transform

구조화된 데이터 변환을 위한 빌더 클래스입니다.

#### Builder Methods

##### builder()
```java
public static TransformBuilder builder()
```
새로운 Transform 빌더를 생성합니다.

#### TransformBuilder Methods

##### rename(String oldName, String newName)
```java
TransformBuilder rename(String oldName, String newName)
```
필드명을 변경합니다.

##### add(String fieldName, Object value)
```java
TransformBuilder add(String fieldName, Object value)
```
새 필드를 추가합니다.

##### remove(String fieldName)
```java
TransformBuilder remove(String fieldName)
```
필드를 제거합니다.

##### when(Predicate<DataRow> condition, Function<DataRow, DataRow> action)
```java
TransformBuilder when(Predicate<DataRow> condition, Function<DataRow, DataRow> action)
```
조건부 변환을 추가합니다.

##### when(Predicate<DataRow> condition, Function<DataRow, DataRow> action, String description)
```java
TransformBuilder when(Predicate<DataRow> condition, Function<DataRow, DataRow> action, String description)
```
설명이 포함된 조건부 변환을 추가합니다.

##### build()
```java
Transform build()
```
Transform 객체를 생성합니다.

**Example:**
```java
Transform transform = Transform.builder()
    .rename("emp_name", "name")
    .add("bonus", 1000)
    .remove("temp_field")
    .when(row -> row.getInt("age") > 50, 
          row -> row.set("category", "senior"))
    .build();
```

---

### DataRow

개별 데이터 행을 나타내는 클래스입니다.

#### Getter Methods

##### getString(String fieldName)
```java
String getString(String fieldName)
```
문자열 값을 가져옵니다.

##### getInt(String fieldName)
```java
Integer getInt(String fieldName)
```
정수 값을 가져옵니다.

##### getDouble(String fieldName)
```java
Double getDouble(String fieldName)
```
실수 값을 가져옵니다.

##### getBoolean(String fieldName)
```java
Boolean getBoolean(String fieldName)
```
불린 값을 가져옵니다.

##### get(String fieldName)
```java
Object get(String fieldName)
```
원시 객체 값을 가져옵니다.

#### Setter Methods

##### set(String fieldName, Object value)
```java
void set(String fieldName, Object value)
```
필드 값을 설정합니다.

#### Utility Methods

##### has(String fieldName)
```java
boolean has(String fieldName)
```
필드가 존재하는지 확인합니다.

##### getFieldNames()
```java
Set<String> getFieldNames()
```
모든 필드명을 가져옵니다.

---

### Format

지원되는 데이터 형식을 나타내는 열거형입니다.

```java
public enum Format {
    CSV,
    JSON
}
```

---

### Exceptions

#### ParseException
```java
public class ParseException extends RuntimeException
```
데이터 파싱 중 발생하는 예외입니다.

#### TransformException
```java
public class TransformException extends RuntimeException
```
데이터 변환 중 발생하는 예외입니다.

#### WriteException
```java
public class WriteException extends RuntimeException
```
데이터 쓰기 중 발생하는 예외입니다.