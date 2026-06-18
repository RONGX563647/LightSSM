package com.lightframework.spi.hutool;

import com.lightframework.spi.validate.Validator;

import java.util.LinkedHashSet;
import java.util.Set;

public class HutoolValidator implements Validator {

    @Override
    public Set<ConstraintViolation> validate(Object obj) {
        Set<ConstraintViolation> violations = new LinkedHashSet<>();
        if (obj == null) {
            violations.add(new ConstraintViolation("", "Object is null", null));
            return violations;
        }
        try {
            java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
            }
        } catch (Exception e) {
            violations.add(new ConstraintViolation("", e.getMessage(), obj));
        }
        return violations;
    }

    @Override
    public Set<ConstraintViolation> validateProperty(Object obj, String propertyName) {
        Set<ConstraintViolation> violations = new LinkedHashSet<>();
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            Object value = field.get(obj);
        } catch (Exception e) {
            violations.add(new ConstraintViolation(propertyName, e.getMessage(), null));
        }
        return violations;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
