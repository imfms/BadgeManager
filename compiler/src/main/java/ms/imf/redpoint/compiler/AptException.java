package ms.imf.redpoint.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

class AptException extends Exception {

    public final Element e;
    public final AnnotationMirror a;
    public final AnnotationValue v;

    public AptException(String message) {
        this(message, (Throwable) null);
    }
    public AptException(Throwable cause) {
        this(null, cause);
    }
    public AptException(String message, Element e) {
        this(message, null, e);
    }
    public AptException(String message, Element e, AnnotationMirror a) {
        this(message, null, e, a);
    }
    public AptException(String message, Element e, AnnotationMirror a, AnnotationValue v) {
        this(message, null, e, a, v);
    }
    public AptException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public AptException(String message, Throwable cause, Element e) {
        this(message, cause, e, null);
    }

    public AptException(String message, Throwable cause, Element e, AnnotationMirror a) {
        this(message, cause, e, a, null);
    }

    public AptException(String message, Throwable cause, Element e, AnnotationMirror a, AnnotationValue v) {
        super(message, cause);

        if ((cause instanceof AptException)
                && e == null
                && a == null
                && v == null) {
            this.e = ((AptException) cause).e;
            this.a = ((AptException) cause).a;
            this.v = ((AptException) cause).v;
        } else {
            this.e = e;
            this.a = a;
            this.v = v;
        }
    }

}
