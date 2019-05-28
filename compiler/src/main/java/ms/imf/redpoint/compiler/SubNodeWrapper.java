package ms.imf.redpoint.compiler;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import ms.imf.redpoint.annotation.Node;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;
import ms.imf.redpoint.annotation.SubNode3;

abstract class SubNodeWrapper
        <SourceNodeType extends Annotation, SubNodeType extends Annotation> {

    static SubNodeWrapper<Node, SubNode> instance(Node source, AnnotationMirror sourceMirror) {
        return new NodeParser(source, sourceMirror);
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
    abstract String[] args();
    abstract SubNodeType[] subNodes();
    protected abstract SubNodeWrapper<SubNodeType, ?> subNodeWrapper(SubNodeType source, AnnotationMirror sourceMirror);

    private static class NodeParser extends SubNodeWrapper<Node, SubNode> {
        private NodeParser(Node source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected String[] args() { return source().args(); }
        @Override protected SubNode[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode, SubNode2> subNodeWrapper(SubNode source, AnnotationMirror sourceMirror) { return new SubNodeParser(source, sourceMirror); }
    }
    private static class SubNodeParser extends SubNodeWrapper<SubNode, SubNode2> {
        private SubNodeParser(SubNode source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected String[] args() { return source().args(); }
        @Override protected SubNode2[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode2, SubNode3> subNodeWrapper(SubNode2 source, AnnotationMirror sourceMirror) { return new SubNode2Parser(source, sourceMirror); }
    }
    private static class SubNode2Parser extends SubNodeWrapper<SubNode2, SubNode3> {
        private SubNode2Parser(SubNode2 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected String[] args() { return source().args(); }
        @Override protected SubNode3[] subNodes() { return source().subNodes(); }
        @Override protected SubNodeWrapper<SubNode3, Annotation> subNodeWrapper(SubNode3 source, AnnotationMirror sourceMirror) { return new SubNode3Parser(source, sourceMirror); }
    }
    private static class SubNode3Parser extends SubNodeWrapper<SubNode3, Annotation> {
        private SubNode3Parser(SubNode3 source, AnnotationMirror sourceMirror) { super(source, sourceMirror); }
        @Override protected String type() { return source().type(); }
        @Override protected String[] args() { return source().args(); }
        @Override protected Annotation[] subNodes() { return null; }
        @Override protected SubNodeWrapper<Annotation, Annotation> subNodeWrapper(Annotation source, AnnotationMirror sourceMirror) { return null; }
    }
}