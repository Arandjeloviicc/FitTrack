package com.fittrack.backend.repository.nutrition.meal.projection;

public record CreateSavedMealResult(
        boolean userExists,
        int requestedCount,
        int validCount,
        int insertedCount
) {}
