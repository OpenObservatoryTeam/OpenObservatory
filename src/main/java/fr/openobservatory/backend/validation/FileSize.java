package fr.openobservatory.backend.validation;

import fr.openobservatory.backend.validation.validator.EnumValueConstraintValidator;
import fr.openobservatory.backend.validation.validator.FileSizeConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FileSizeConstraintValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FileSize {

  long min() default 0L;

  long max() default Long.MAX_VALUE;

  // ---

  String message() default "{fr.openobservatory.backend.validation.FileSize.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
