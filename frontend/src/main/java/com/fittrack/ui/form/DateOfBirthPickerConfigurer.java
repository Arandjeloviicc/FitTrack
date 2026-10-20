package com.fittrack.ui.form;

import com.fittrack.config.AppConstants;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;

public final class DateOfBirthPickerConfigurer extends AppDatePickerConfigurer {

    private DateOfBirthPickerConfigurer() {}

    public static void configure(DatePicker picker) {
        LocalDate latestAllowedDateOfBirth =
                LocalDate.now(AppConstants.APP_TIME_ZONE)
                        .minusYears(
                                AppConstants.Validation.MIN_AGE
                        );

        AppDatePickerConfigurer.configure(
                picker,
                latestAllowedDateOfBirth
        );
    }
}