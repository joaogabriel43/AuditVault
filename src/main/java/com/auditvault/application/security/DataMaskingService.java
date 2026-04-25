package com.auditvault.application.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class DataMaskingService {

    private static final List<String> SENSITIVE_KEYS = Arrays.asList(
            "password", "cpf", "cardnumber", "secret", "token"
    );
    private static final String MASK = "***";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @RequiredArgsConstructor
    public static class MaskingResult {
        private final String maskedPayload;
        private final boolean masked;
    }

    public MaskingResult maskJson(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return new MaskingResult(payload, false);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            boolean isMasked = maskNode(rootNode);
            return new MaskingResult(objectMapper.writeValueAsString(rootNode), isMasked);
        } catch (JsonProcessingException e) {
            // Se não for um JSON válido, retorna o payload original com masked=false
            return new MaskingResult(payload, false);
        }
    }

    private boolean maskNode(JsonNode node) {
        boolean masked = false;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode childNode = field.getValue();

                if (isSensitiveKey(key) && childNode.isTextual()) {
                    objectNode.put(key, MASK);
                    masked = true;
                } else {
                    boolean childMasked = maskNode(childNode);
                    if (childMasked) {
                        masked = true;
                    }
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                boolean childMasked = maskNode(element);
                if (childMasked) {
                    masked = true;
                }
            }
        }

        return masked;
    }

    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }
}
