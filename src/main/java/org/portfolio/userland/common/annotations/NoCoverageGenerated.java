package org.portfolio.userland.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation for classes that you want to not be covered by coverage tools.
 * It works because modern coverage tools skip any class with any annotation that has "Generated" in name.
 * <p>Note: due to limitations of coverage tools, you cannot use it in inheritance, even with <code>@Inherited</code>
 * annotation. You need to use it on every class you want to be excluded.</p>
 * <p>If you want to exclude entire packages or many classes at once, you need to configure coverage tools directly.</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NoCoverageGenerated {
}
