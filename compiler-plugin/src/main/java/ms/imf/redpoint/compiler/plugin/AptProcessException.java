package ms.imf.redpoint.compiler.plugin;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * 支持指出错误所在具体代码元素的异常，便于用于定位到异常发生的位置
 *
 * @see Messager#printMessage(Diagnostic.Kind, CharSequence, Element, AnnotationMirror, AnnotationValue)
 */
public class AptProcessException extends Exception {

    /**
     * @see Messager#printMessage(Diagnostic.Kind, CharSequence, Element, AnnotationMirror, AnnotationValue)
     */
    public final Element e;
    /**
     * @see Messager#printMessage(Diagnostic.Kind, CharSequence, Element, AnnotationMirror, AnnotationValue)
     */
    public final AnnotationMirror a;
    /**
     * @see Messager#printMessage(Diagnostic.Kind, CharSequence, Element, AnnotationMirror, AnnotationValue)
     */
    public final AnnotationValue v;

    public AptProcessException(String message) { this(message, (Throwable) null); }
    public AptProcessException(Throwable cause) { this(null, cause); }
    public AptProcessException(String message, Element e) { this(message, null, e); }
    public AptProcessException(String message, Element e, AnnotationMirror a) { this(message, null, e, a); }
    public AptProcessException(String message, Element e, AnnotationMirror a, AnnotationValue v) { this(message, null, e, a, v); }
    public AptProcessException(String message, Throwable cause) { this(message, cause, null); }
    public AptProcessException(String message, Throwable cause, Element e) { this(message, cause, e, null); }
    public AptProcessException(String message, Throwable cause, Element e, AnnotationMirror a) { this(message, cause, e, a, null); }
    public AptProcessException(String message, Throwable cause, Element e, AnnotationMirror a, AnnotationValue v) {
        super(message, cause);

        if (cause instanceof AptProcessException) {
            /*
            由于注解处理器只能指定一份指定错误代码元素，所以应指定更接近实际错误的代码元素位置
            所以如果cause包含代码元素信息的情况下，使用cause的信息，因为cause为更具体的实现代码抛出的，更接近实际错误
             */
            AptProcessException aptCause = (AptProcessException) cause;

            v = aptCause.v != null
                    ? aptCause.v
                    : v;

            a = aptCause.a != null
                    ? aptCause.a
                    : a;

            e = aptCause.e != null
                    ? aptCause.e
                    : e;

        }

        this.e = e;
        this.a = a;
        this.v = v;
    }

}
