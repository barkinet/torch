package com.brightgestures.brightify;

import com.brightgestures.brightify.annotation.Entity;
import com.brightgestures.brightify.annotation.Ignore;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
@SupportedAnnotationTypes({"com.brightgestures.brightify.annotation.Entity"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class BrightifyAnnotationProcessor extends AbstractProcessor {


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Entity.class);
        new EntitiesCreator(elements).processElements();
        return true;
    }

    public class Creator {
        private StringBuilder mBuilder = new StringBuilder();
        private int mLevel = 0;

        protected Creator append(Object value) {
            mBuilder.append(value);
            return this;
        }

        protected Creator line(Object value) {
            emptyLine();

            for(int i = 0; i < mLevel; i++) {
                mBuilder.append("    ");
            }
            mBuilder.append(value);

            return this;
        }

        protected Creator emptyLine() {
            mBuilder.append("\n");
            return this;
        }

        protected Creator nest() {
            mBuilder.append(" {");
            mLevel++;
            return this;
        }

        protected Creator unNest() {
            mLevel--;
            line("}");
            return this;
        }

        protected void save(String name) {
            Writer writer = null;
            try {
                writer = processingEnv.getFiler().createSourceFile(name).openWriter();

                writer.write(mBuilder.toString());

                writer.flush();
                writer.close();

            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
    }

    public class EntitiesCreator extends Creator {
        private final Set<? extends Element> mElements;

        public EntitiesCreator(Set<? extends Element> elements) {
            mElements = elements;
        }

        public void processElements() {

            Map<Element, Boolean> ignored = new HashMap<Element, Boolean>();
            for(Element element : mElements) {
                boolean ignore = new EntityMapperCreator(element).processEntity();

                ignored.put(element, ignore);
            }


            append("/* Generated on ").append(new Date()).append(" by BrightifyAnnotationProcessor */");
            line("package com.brightgestures.brightify;");
            emptyLine();
            line("import java.util.Map;");
            line("import java.util.HashMap;");
            for(Element element : mElements) {
                if(ignored.get(element)) {
                    continue;
                }

                line("import ").append(element).append(";");
                line("import ").append(element).append("Metadata").append(";");
            }
            emptyLine();
            line("public class Entities").nest();
            line("private static Map<Class, EntityMetadata> sMetadatas = new HashMap<Class, EntityMetadata>();");
            emptyLine();
            line("static").nest();
            for(Element element : mElements) {
                if(ignored.get(element)) {
                    continue;
                }

                line("sMetadatas.put(").append(element.getSimpleName()).append(".class, new ")
                        .append(element.getSimpleName()).append("Metadata());");
            }
            unNest();
            unNest();

            save("com.brightgestures.brightify.Entities");
        }


    }

    public class EntityMapperCreator extends Creator {
        private final Element mElement;
        public EntityMapperCreator(Element element) {
            mElement = element;
        }

        public boolean processEntity() {
            Entity entityAnnotation = mElement.getAnnotation(Entity.class);
            if(entityAnnotation.ignore()) {
                return true;
            }

            String className = mElement.toString();

            String entityName = mElement.getSimpleName().toString();
            String entityFullName = mElement.toString();
            String metadataName = mElement.getSimpleName() + "Metadata";
            String metadataFullName = "com.brightgestures.brightify.metadata." + metadataName;


            append("/* Generated on ").append(new Date()).append(" by BrightifyAnnotationProcessor */");
            line("package com.brightgestures.brightify.metadata;");
            emptyLine();
            line("import ").append(entityFullName).append(";");
            emptyLine();
            line("import android.database.Cursor;");
            line("import android.content.ContentValues;");
            line("import com.brightgestures.brightify.EntityMetadata;");
            emptyLine();
            line("public class ").append(metadataName).append(" extends EntityMetadata<").append(entityName).append(">").nest();
            emptyLine();
            line("@Override");
            line("public ").append(entityName).append(" createFromCursor(Cursor cursor)").nest();
            line(entityName).append(" entity = new ").append(entityName).append("();");
            emptyLine();
            List<? extends Element> children = mElement.getEnclosedElements();

            Types types = processingEnv.getTypeUtils();
            Elements elements = processingEnv.getElementUtils();

            Map<Element, Boolean> readableProperties;
            Map<Element, String> fieldSetterMapping;

            List<Property> properties = new ArrayList<Property>();

            for(Element child : children) {
                Ignore ignore = child.getAnnotation(Ignore.class);
                if(ignore != null) {
                    continue;
                }

                String childName = child.toString();
                if(child.getKind() == ElementKind.METHOD && (childName.startsWith("set"))) {

                }

                if(child.getKind().isField()) {
                    line("int ").append(child).append("Index = cursor.getColumnIndex(\"").append(child).append("\");");
                    line("entity.").append(child).append(" = cursor.get(").append(child).append("Index);");
                }/*

                if(TypeUtils.isAssignableFrom(Boolean.class, type)) {
                    value = cursor.getInt(index) > 0;
                } else if(TypeUtils.isAssignableFrom(Byte.class, type)) {
                    value = (byte) cursor.getInt(index);
                } else if(TypeUtils.isAssignableFrom(Short.class, type)) {
                    value = cursor.getShort(index);
                } else if(TypeUtils.isAssignableFrom(Integer.class, type)) {
                    value = cursor.getInt(index);
                } else if(TypeUtils.isAssignableFrom(Long.class, type)) {
                    value = cursor.getLong(index);
                } else if(TypeUtils.isAssignableFrom(Float.class, type)) {
                    value = cursor.getFloat(index);
                } else if(TypeUtils.isAssignableFrom(Double.class, type)) {
                    value = cursor.getDouble(index);
                } else if(TypeUtils.isAssignableFrom(String.class, type)) {
                    value = cursor.getString(index);
                } else if(TypeUtils.isAssignableFrom(byte[].class, type)) {
                    value = cursor.getBlob(index);
                }  else if(TypeUtils.isAssignableFrom(Key.class, type)) {
                    throw new UnsupportedOperationException("Not implemented!");
                } else if(TypeUtils.isAssignableFrom(Ref.class, type)) {
                    throw new UnsupportedOperationException("Not implemented!");
                } else if(TypeUtils.isAssignableFrom(Serializable.class, type)) {
                    try {
                        value = Serializer.deserialize(cursor.getBlob(index));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new IllegalStateException("Type '" + type.toString() + "' cannot be restored from database!");
                }
*/
               // processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, child + " of type " + child.asType(), child);

            }

            emptyLine();
            line("return entity;");
            unNest();

            line("@Override");
            line("public ContentValues toContentValues(").append(entityName).append(" entity)").nest();
            line("ContentValues values = new ContentValues();");

            for(Element child : children) {
                Ignore ignore = child.getAnnotation(Ignore.class);
                if(ignore != null) {
                    continue;
                }

                String childName = child.toString();
                String columnName = childName;

                if(child.getKind() == ElementKind.METHOD) {
                    // && (childName.startsWith("get") || childName.startsWith("is")
                } else if(child.getKind().isField()) {
                    TypeMirror childType = child.asType();

                    if(childType == typeOf(Key.class)) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Keys are not yet supported!", child);
                    } else if(childType == typeOf(Ref.class)) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Refs are not yet supported!", child);
                    } else if(
                                childType == typeOf(Boolean.class) ||
                                childType == typeOf(boolean.class) ||
                                childType == typeOf(Byte.class) ||
                                childType == typeOf(byte.class) ||
                                childType == typeOf(Short.class) ||
                                childType == typeOf(short.class) ||
                                childType == typeOf(Integer.class) ||
                                childType == typeOf(int.class) ||
                                childType == typeOf(Long.class) ||
                                childType == typeOf(long.class) ||
                                childType == typeOf(Float.class) ||
                                childType == typeOf(float.class) ||
                                childType == typeOf(Double.class) ||
                                childType == typeOf(double.class) ||
                                childType == typeOf(String.class) ||
                                childType == typeOf(byte[].class)) {
                        line("values.put(\"").append(columnName).append("\", entity.").append(childName).append(");");
                    } else if(types.isAssignable(childType, elements.getTypeElement("java.io.Serializable").asType())) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializable objects not yet supported!", child);
                    } else {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Type '" + child.asType() + "' is not supported!");
                    }

                    //processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, child + " assignable " + (child.asType() == elements.getTypeElement("java.lang.Long") ? "yes" : "no"), child);
                }
//

                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, child + " of type " + child.asType(), child);
            }

            line("return values;");
            unNest();
            unNest();

            save(metadataFullName);

            return false;
        }

        private TypeMirror typeOf(Class cls) {


            Element el = processingEnv.getElementUtils().getTypeElement(cls.getName());
            return  el.asType();
        }
    }

    public static class Property {
        Element element;
        boolean writable;
        Type type;
        String name;

        enum Type {
            FIELD, METHOD
        }
    }

}
