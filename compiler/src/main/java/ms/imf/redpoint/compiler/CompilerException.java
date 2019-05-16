package ms.imf.redpoint.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

class CompilerException extends Exception {

    public final Element e;
    public final AnnotationMirror a;
    public final AnnotationValue v;

    public CompilerException(String message) {
        this(message, (Throwable) null);
    }
    public CompilerException(Throwable cause) {
        this(null, cause);
    }
    public CompilerException(String message, Element e) {
        this(message, null, e);
    }
    public CompilerException(String message, Element e, AnnotationMirror a) {
        this(message, null, e, a);
    }
    public CompilerException(String message, Element e, AnnotationMirror a, AnnotationValue v) {
        this(message, null, e, a, v);
    }
    public CompilerException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public CompilerException(String message, Throwable cause, Element e) {
        this(message, cause, e, null);
    }

    public CompilerException(String message, Throwable cause, Element e, AnnotationMirror a) {
        this(message, cause, e, a, null);
    }

    public CompilerException(String message, Throwable cause, Element e, AnnotationMirror a, AnnotationValue v) {
        super(message, cause);

        if ((cause instanceof CompilerException)
                && e == null
                && a == null
                && v == null) {
            this.e = ((CompilerException) cause).e;
            this.a = ((CompilerException) cause).a;
            this.v = ((CompilerException) cause).v;
        } else {
            this.e = e;
            this.a = a;
            this.v = v;
        }
    }

}
