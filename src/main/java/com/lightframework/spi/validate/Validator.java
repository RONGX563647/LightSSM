package com.lightframework.spi.validate;

import com.lightframework.spi.Ordered;
import java.util.Set;

public interface Validator extends Ordered {

    Set<ConstraintViolation> validate(Object obj);

    Set<ConstraintViolation> validateProperty(Object obj, String propertyName);

    @Override
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }

    class ConstraintViolation {
        private final String propertyPath;
        private final String message;
        private final Object invalidValue;

        public ConstraintViolation(String propertyPath, String message, Object invalidValue) {
            this.propertyPath = propertyPath;
            this.message = message;
            this.invalidValue = invalidValue;
        }

        public String getPropertyPath() { return propertyPath; }
        public String getMessage() { return message; }
        public Object getInvalidValue() { return invalidValue; }

        @Override
        public String toString() {
            return propertyPath + ": " + message;
        }
    }
}
