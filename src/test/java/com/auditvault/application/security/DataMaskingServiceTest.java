package com.auditvault.application.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMaskingServiceTest {

    private DataMaskingService dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingService();
    }

    @Test
    void shouldMaskSensitiveKeysInJson() {
        String inputJson = "{\"name\":\"John Doe\",\"password\":\"secret123\",\"email\":\"john@doe.com\",\"cpf\":\"123.456.789-00\",\"cardNumber\":\"1234-5678-9012-3456\"}";
        
        DataMaskingService.MaskingResult result = dataMaskingService.maskJson(inputJson);
        
        assertTrue(result.isMasked());
        
        String maskedJson = result.getMaskedPayload();
        assertTrue(maskedJson.contains("\"password\":\"***\""));
        assertTrue(maskedJson.contains("\"cpf\":\"***\""));
        assertTrue(maskedJson.contains("\"cardNumber\":\"***\""));
        assertTrue(maskedJson.contains("\"name\":\"John Doe\"")); // Shouldn't be masked
        assertTrue(maskedJson.contains("\"email\":\"john@doe.com\"")); // Shouldn't be masked
    }

    @Test
    void shouldNotAlterJsonWithoutSensitiveData() {
        String inputJson = "{\"name\":\"John Doe\",\"age\":30}";
        
        DataMaskingService.MaskingResult result = dataMaskingService.maskJson(inputJson);
        
        assertFalse(result.isMasked());
        assertEquals(inputJson, result.getMaskedPayload());
    }
    
    @Test
    void shouldHandleNullOrEmptyInput() {
        DataMaskingService.MaskingResult resultNull = dataMaskingService.maskJson(null);
        assertFalse(resultNull.isMasked());
        assertNull(resultNull.getMaskedPayload());

        DataMaskingService.MaskingResult resultEmpty = dataMaskingService.maskJson("");
        assertFalse(resultEmpty.isMasked());
        assertEquals("", resultEmpty.getMaskedPayload());
    }
}
