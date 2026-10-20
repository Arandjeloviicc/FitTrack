package com.fittrack.ui.control;

import com.fittrack.ui.form.TextFieldValidators;
import javafx.scene.control.TextField;

public class IntegerTextField extends TextField {

    public IntegerTextField() {
        super();
        TextFieldValidators.applyIntegerFilter(this);
    }
}