package dev.ancaria.coderpack.verify;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * One class inside the jar, as much of it as this linter needs.
 *
 * <p>Read, never loaded. Loading a class runs its static initialiser, and a
 * linter that might be pointed at a jar somebody else built has no business
 * running any of it. Only the header, the constructors and the
 * {@code @Subscribe} methods are kept, so a fat jar of a few thousand classes
 * stays cheap.
 *
 * @param version the class file major, 65 for Java 21
 * @param parsed  false when the class file is newer than the ASM in here can
 *                read. Everything but the name and the version is empty then,
 *                and a check that needs more says so instead of guessing.
 */
record Klass(String name, int access, String superName, List<String> interfaces,
             int version, List<Meth> methods, boolean parsed) {

    /** A constructor or a listener. Nothing else is kept. */
    record Meth(String name, String descriptor, int access, boolean subscribes) {

        boolean isPublic() {
            return Modifier.isPublic(access);
        }
    }

    boolean isPublic() {
        return Modifier.isPublic(access);
    }

    boolean isAbstract() {
        return Modifier.isAbstract(access) || Modifier.isInterface(access);
    }

    /** Dotted, the way a descriptor and a stack trace spell it. */
    String dotted() {
        return name.replace('/', '.');
    }

    static Klass read(String entry, byte[] bytes) {
        int version = bytes.length > 7 ? ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff) : 0;
        try {
            Reader reader = new Reader(version);
            new ClassReader(bytes).accept(reader,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return reader.klass;
        } catch (RuntimeException unreadable) {
            String name = entry.substring(0, entry.length() - ".class".length());
            return new Klass(name, 0, null, List.of(), version, List.of(), false);
        }
    }

    private static final class Reader extends ClassVisitor {

        private static final String SUBSCRIBE = "Ldev/ancaria/coderpack/api/Subscribe;";

        private final int version;
        private final List<Meth> methods = new ArrayList<>();

        private Klass klass;

        Reader(int version) {
            super(Opcodes.ASM9);
            this.version = version;
        }

        @Override
        public void visit(int ignored, int access, String name, String signature,
                          String superName, String[] interfaces) {
            klass = new Klass(name, access, superName,
                              interfaces == null ? List.of() : List.of(interfaces),
                              version, methods, true);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {

                private boolean subscribes;

                @Override
                public AnnotationVisitor visitAnnotation(String type, boolean visible) {
                    subscribes |= SUBSCRIBE.equals(type);
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (subscribes || "<init>".equals(name)) {
                        methods.add(new Meth(name, descriptor, access, subscribes));
                    }
                }
            };
        }
    }
}
