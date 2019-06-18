package ms.imf.redpoint.compiler.plugin;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

public class AptProcessException extends Exception {

    public final Element e;
    public final AnnotationMirror a;
    public final AnnotationValue v;

    public AptProcessException(String message) {
        this(message, (Throwable) null);
    }
    public AptProcessException(Throwable cause) {
        this(null, cause);
    }
    public AptProcessException(String message, Element e) {
        this(message, null, e);
    }
    public AptProcessException(String message, Element e, AnnotationMirror a) {
        this(message, null, e, a);
    }
    public AptProcessException(String message, Element e, AnnotationMirror a, AnnotationValue v) {
        this(message, null, e, a, v);
    }
    public AptProcessException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public AptProcessException(String message, Throwable cause, Element e) {
        this(message, cause, e, null);
    }

    public AptProcessException(String message, Throwable cause, Element e, AnnotationMirror a) {
        this(message, cause, e, a, null);
    }

    public AptProcessException(String message, Throwable cause, Element e, AnnotationMirror a, AnnotationValue v) {
        super(message, cause);

        if ((cause instanceof AptProcessException)
                && e == null
                && a == null
                && v == null) {
            this.e = ((AptProcessException) cause).e;
            this.a = ((AptProcessException) cause).a;
            this.v = ((AptProcessException) cause).v;
        } else {
            this.e = e;
            this.a = a;
            this.v = v;
        }
    }

}
