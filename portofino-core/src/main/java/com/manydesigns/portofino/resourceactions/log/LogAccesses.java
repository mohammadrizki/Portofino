package com.manydesigns.portofino.resourceactions.log;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@Documented
public @interface LogAccesses {

    boolean value() default true;

}
