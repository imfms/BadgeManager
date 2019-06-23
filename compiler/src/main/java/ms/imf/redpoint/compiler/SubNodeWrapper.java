package ms.imf.redpoint.compiler;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import ms.imf.redpoint.annotation.NodeArg;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;
import ms.imf.redpoint.annotation.SubNode3;
import ms.imf.redpoint.annotation.SubNode4;
import ms.imf.redpoint.annotation.SubNode5;
import ms.imf.redpoint.annotation.SubNode6;
import ms.imf.redpoint.annotation.SubNode7;
import ms.imf.redpoint.annotation.SubNode8;

abstract class SubNodeWrapper
        <SourceNodeType extends Annotation, SubNodeType extends Annotation> {

    static SubNodeWrapper<SubNode, SubNode2> instance(SubNode source, AnnotationMirror sourceMirror) {
        return new SubNode1Parser(source, sourceMirror);
    }

    private final SourceNodeType source;
    private final AnnotationMirror sourceMirror;

    private SubNodeWrapper(SourceNodeType source, AnnotationMirror sourceMirror) {
        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }
        if (sourceMirror == null) {
            throw new IllegalArgumentException("sourceMirror can't be null");
        }
        this.source = source;
        this.sourceMirror = sourceMirror;
    }

    final SourceNodeType source() { return source; }
    final AnnotationMirror sourceMirror() { return sourceMirror; }
    final List<AnnotationMirror> subNodeMirrors() { return PathNodeAnnotationParser.getAnnotionMirrorValue(sourceMirror(), "subNodes"); }
    final TypeElement subRef() {
        DeclaredType subRefType = PathNodeAnnotationParser.getAnnotionMirrorValue(sourceMirror(), "subRef");
        return subRefType != null
                ? (TypeElement) subRefType.asElement()
                : null;
    }

    abstract String type();
    abstract NodeArg[] args();
    abstract SubNodeType[] subNodes();
    protected abstract SubNodeWrapper<SubNodeType, ?> subNodeWrapper(SubNodeType source, AnnotationMirror sourceMirror);

    private static class SubNode1Parser extends SubNodeWrapper<SubNode, SubNode2> {
        private SubNode1Parser(SubNode source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode2[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode2, ?> subNodeWrapper(SubNode2 source, AnnotationMirror sourceMirror) { return new SubNode2Parser(source, sourceMirror); }
    }
    private static class SubNode2Parser extends SubNodeWrapper<SubNode2, SubNode3> {
        private SubNode2Parser(SubNode2 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode3[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode3, ?> subNodeWrapper(SubNode3 source, AnnotationMirror sourceMirror) { return new SubNode3Parser(source, sourceMirror); }
    }
    private static class SubNode3Parser extends SubNodeWrapper<SubNode3, SubNode4> {
        private SubNode3Parser(SubNode3 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode4[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode4, ?> subNodeWrapper(SubNode4 source, AnnotationMirror sourceMirror) { return new SubNode4Parser(source, sourceMirror); }
    }
    private static class SubNode4Parser extends SubNodeWrapper<SubNode4, SubNode5> {
        private SubNode4Parser(SubNode4 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode5[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode5, ?> subNodeWrapper(SubNode5 source, AnnotationMirror sourceMirror) { return new SubNode5Parser(source, sourceMirror); }
    }
    private static class SubNode5Parser extends SubNodeWrapper<SubNode5, SubNode6> {
        private SubNode5Parser(SubNode5 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode6[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode6, ?> subNodeWrapper(SubNode6 source, AnnotationMirror sourceMirror) { return new SubNode6Parser(source, sourceMirror); }
    }
    private static class SubNode6Parser extends SubNodeWrapper<SubNode6, SubNode7> {
        private SubNode6Parser(SubNode6 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode7[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode7, ?> subNodeWrapper(SubNode7 source, AnnotationMirror sourceMirror) { return new SubNode7Parser(source, sourceMirror); }
    }
    private static class SubNode7Parser extends SubNodeWrapper<SubNode7, SubNode8> {
        private SubNode7Parser(SubNode7 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected SubNode8[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode8, ?> subNodeWrapper(SubNode8 source, AnnotationMirror sourceMirror) { return new SubNode8Parser(source, sourceMirror); }
    }
    private static class SubNode8Parser extends SubNodeWrapper<SubNode8, Annotation> {
        private SubNode8Parser(SubNode8 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected NodeArg[] args() { return source().args(); }
        @Override protected Annotation[] subNodes() { return null; }
        @Override protected SubNodeWrapper<Annotation, Annotation> subNodeWrapper(Annotation source, AnnotationMirror sourceMirror) { return null; }
    }


}