package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface PathAptGlobalConfig {

    String convertCheckConfigFilePath() default "";

    /**
     * format: packageName/resourceName
     */
    String nodeSchemaExportJsonJavaStyleResource() default "";

}
