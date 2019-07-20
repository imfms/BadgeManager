package ms.imf.redpoint.compiler;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import ms.imf.redpoint.annotation.Arg;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;
import ms.imf.redpoint.annotation.SubNode3;
import ms.imf.redpoint.annotation.SubNode4;
import ms.imf.redpoint.annotation.SubNode5;
import ms.imf.redpoint.annotation.SubNode6;
import ms.imf.redpoint.annotation.SubNode7;
import ms.imf.redpoint.annotation.SubNode8;

/**
 * 由于注解类型不支持嵌套，所以通过创建独立注解类型层层上下级引用的方式进行规避。
 * 为使注解处理器方便解析，对这些独立注解抽取出包装类型，对独立注解的不同实现细节进行统一
 */
abstract class AnnotationNodeWrapper
        <SourceNodeType extends Annotation, SubNodeType extends Annotation> {

    static AnnotationNodeWrapper<SubNode, SubNode2> instance(SubNode source, AnnotationMirror sourceMirror) {
        return new SubNode1Parser(source, sourceMirror);
    }

    private final SourceNodeType source;
    private final AnnotationMirror sourceMirror;

    private AnnotationNodeWrapper(SourceNodeType source, AnnotationMirror sourceMirror) {
        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }
        if (sourceMirror == null) {
            throw new IllegalArgumentException("sourceMirror can't be null");
        }
        this.source = source;
        this.sourceMirror = sourceMirror;
    }

    SourceNodeType source() { return source; }
    AnnotationMirror sourceMirror() { return sourceMirror; }

    /**
     * @see SubNode#value()
     */
    abstract String name();
    /**
     * @see SubNode#args()
     */
    abstract Arg[] args();
    /**
     * @see SubNode#subNodes()
     */
    abstract SubNodeType[] subNodes();
    /**
     * @see SubNode#subNodes()
     */
    final List<AnnotationMirror> subNodeMirrors() { return NodeContainerAnnotationParser.getAnnotationMirrorValue(sourceMirror(), "subNodes"/* todo runtime check */); }
    /**
     * @see SubNode#subNodeContainerRef()
     */
    final TypeElement subRef() {
        DeclaredType subRefType = NodeContainerAnnotationParser.getAnnotationMirrorValue(sourceMirror(), "subNodeContainerRef" /* todo runtime check */);
        return subRefType != null
                ? (TypeElement) subRefType.asElement()
                : null;
    }

    /**
     * 转换自己引用的子注解类型为本通用包装类型
     * 像链表一样每个类型引用下个类型
     */
    protected abstract AnnotationNodeWrapper<SubNodeType, ?> subNodeWrapper(SubNodeType source, AnnotationMirror sourceMirror);

    private static class SubNode1Parser extends AnnotationNodeWrapper<SubNode, SubNode2> {
        private SubNode1Parser(SubNode source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode2[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode2, ?> subNodeWrapper(SubNode2 source, AnnotationMirror sourceMirror) { return new SubNode2Parser(source, sourceMirror); }
    }
    private static class SubNode2Parser extends AnnotationNodeWrapper<SubNode2, SubNode3> {
        private SubNode2Parser(SubNode2 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode3[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode3, ?> subNodeWrapper(SubNode3 source, AnnotationMirror sourceMirror) { return new SubNode3Parser(source, sourceMirror); }
    }
    private static class SubNode3Parser extends AnnotationNodeWrapper<SubNode3, SubNode4> {
        private SubNode3Parser(SubNode3 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode4[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode4, ?> subNodeWrapper(SubNode4 source, AnnotationMirror sourceMirror) { return new SubNode4Parser(source, sourceMirror); }
    }
    private static class SubNode4Parser extends AnnotationNodeWrapper<SubNode4, SubNode5> {
        private SubNode4Parser(SubNode4 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode5[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode5, ?> subNodeWrapper(SubNode5 source, AnnotationMirror sourceMirror) { return new SubNode5Parser(source, sourceMirror); }
    }
    private static class SubNode5Parser extends AnnotationNodeWrapper<SubNode5, SubNode6> {
        private SubNode5Parser(SubNode5 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode6[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode6, ?> subNodeWrapper(SubNode6 source, AnnotationMirror sourceMirror) { return new SubNode6Parser(source, sourceMirror); }
    }
    private static class SubNode6Parser extends AnnotationNodeWrapper<SubNode6, SubNode7> {
        private SubNode6Parser(SubNode6 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode7[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode7, ?> subNodeWrapper(SubNode7 source, AnnotationMirror sourceMirror) { return new SubNode7Parser(source, sourceMirror); }
    }
    private static class SubNode7Parser extends AnnotationNodeWrapper<SubNode7, SubNode8> {
        private SubNode7Parser(SubNode7 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected SubNode8[] subNodes() { return source().subNodes(); }
        @Override protected AnnotationNodeWrapper<SubNode8, ?> subNodeWrapper(SubNode8 source, AnnotationMirror sourceMirror) { return new SubNode8Parser(source, sourceMirror); }
    }
    private static class SubNode8Parser extends AnnotationNodeWrapper<SubNode8, Annotation> {
        private SubNode8Parser(SubNode8 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String name() { return source().value(); }
        @Override protected Arg[] args() { return source().args(); }
        @Override protected Annotation[] subNodes() { return null; }
        @Override protected AnnotationNodeWrapper<Annotation, Annotation> subNodeWrapper(Annotation source, AnnotationMirror sourceMirror) { return null; }
    }
}