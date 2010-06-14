/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author sea36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LensfieldParameter {
    String name() default "";
    boolean required() default false;
}
