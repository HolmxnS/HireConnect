package com.project.HireConnect.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;


import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {

    String message() default "Debe ser mayor o igual a 18 años";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
