package com.aidevplanner.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class AllowedGoalDurationDaysValidator implements ConstraintValidator<AllowedGoalDurationDays, Integer> {

    private static final Set<Integer> ALLOWED_DAYS = Set.of(14, 21);

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || ALLOWED_DAYS.contains(value);
    }
}
