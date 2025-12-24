package me.hanju.branchdown.config;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/** Map<String, Object>을 JSON 문자열로 변환하는 JPA Converter */
@Slf4j
@Converter
public class JsonConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("Failed to convert Map to JSON string", e);
      throw new IllegalStateException("Error converting Map to JSON", e);
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {
      });
    } catch (JsonProcessingException e) {
      log.error("Failed to convert JSON string to Map", e);
      throw new IllegalStateException("Error converting JSON to Map", e);
    }
  }
}
