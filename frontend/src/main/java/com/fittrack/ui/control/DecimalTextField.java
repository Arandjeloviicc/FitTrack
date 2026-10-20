package com.fittrack.ui.control;

import com.fittrack.ui.form.TextFieldValidators;
import javafx.scene.control.TextField;

public class DecimalTextField extends TextField {

    private static final int MAX_DECIMALS = 3;

    public DecimalTextField() {
        super();
        TextFieldValidators.applyDecimalFilter(this, MAX_DECIMALS);
    }
}