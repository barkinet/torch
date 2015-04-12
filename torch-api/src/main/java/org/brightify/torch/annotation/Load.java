package org.brightify.torch.annotation;

import org.brightify.torch.filter.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:tadeas@brightify.org">Tadeas Kriz</a>
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Load {
    Class<?>[] when() default { };

    Class<?>[] unless() default { };

    class LoadFeature implements Property.Feature {
        private final Class<?>[] when;

        private final Class<?>[] unless;

        public LoadFeature(Class<?>[] when, Class<?>[] unless) {
            this.when = when;
            this.unless = unless;
        }

        public Class<?>[] getWhen() {
            return when;
        }

        public Class<?>[] getUnless() {
            return unless;
        }
    }
}
