package me.hanju.branchdown.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Integer 리스트를 쉼표로 구분된 문자열로 변환하는 JPA Converter */
@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {

  private static final String DELIMITER = ",";

  @Override
  public String convertToDatabaseColumn(List<Integer> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "";
    }
    return attribute.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(DELIMITER));
  }

  @Override
  public List<Integer> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.strip().isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.stream(dbData.split(DELIMITER))
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .map(Integer::valueOf)
        .toList();
  }
}
