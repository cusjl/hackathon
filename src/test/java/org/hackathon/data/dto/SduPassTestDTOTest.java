package org.hackathon.data.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SduPassTestDTOTest {

    @Test
    void rejectsCasIdLongerThanTheStudentColumn() {
        SduPassTestDTO dto = new SduPassTestDTO();
        dto.setCasId("2026000000001");
        dto.setName("学生");

        assertFalse(jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()
                .validate(dto).isEmpty());
    }
}
