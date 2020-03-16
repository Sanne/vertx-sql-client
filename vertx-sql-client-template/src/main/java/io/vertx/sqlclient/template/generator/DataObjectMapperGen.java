package io.vertx.sqlclient.template.generator;

import io.vertx.codegen.DataObjectModel;
import io.vertx.codegen.Generator;
import io.vertx.codegen.MapperKind;
import io.vertx.codegen.PropertyInfo;
import io.vertx.codegen.PropertyKind;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.type.AnnotationValueInfo;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.DataObjectInfo;
import io.vertx.codegen.type.MapperInfo;
import io.vertx.codegen.type.PrimitiveTypeInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.sqlclient.template.annotations.Mapped;
import io.vertx.sqlclient.template.annotations.TemplateParam;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DataObjectMapperGen extends Generator<DataObjectModel> {

  public DataObjectMapperGen() {
    kinds = Collections.singleton("dataObject");
    name = "data_object_mappers";
  }

  @Override
  public Collection<Class<? extends Annotation>> annotations() {
    return Collections.singletonList(DataObject.class);
  }

  @Override
  public String filename(DataObjectModel model) {
    if (model.isClass()) {
      Optional<String> mapped = model
        .getAnnotations()
        .stream().filter(ann -> ann.getName().equals(Mapped.class.getName()))
        .findFirst()
        .map(ann -> model.getFqn() + "Mapper.java");
      return mapped.orElse(null);
    }
    return null;
  }

  @Override
  public String render(DataObjectModel model, int index, int size, Map<String, Object> session) {
    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    String visibility= model.isPublicConverter() ? "public" : "";

    writer.print("package " + model.getType().getPackageName() + ";\n");
    writer.print("\n");
    writer.print("/**\n");
    writer.print(" * Mapper for {@link " + model.getType().getSimpleName() + "}.\n");
    writer.print(" * NOTE: This class has been automatically generated from the {@link " + model.getType().getSimpleName() + "} original class using Vert.x codegen.\n");
    writer.print(" */\n");
    writer.print("public class " + model.getType().getSimpleName() + "Mapper {\n");
    genFromRow(visibility, model, writer);
    genToParams(visibility, model, writer);
    writer.print("}\n");
    return buffer.toString();
  }

  private void genFromRow(String visibility, DataObjectModel model, PrintWriter writer) {
    writer.print("\n");
    writer.print("  " + visibility + " static final java.util.function.Function<io.vertx.sqlclient.Row, " + model.getType().getSimpleName() + "> READER = " + model.getType().getSimpleName() + "Mapper::fromRow;\n");
    writer.print("\n");
    writer.print("  " + visibility + " static " + model.getType().getSimpleName() + " fromRow(io.vertx.sqlclient.Row row) {\n");
    writer.print("    " + model.getType().getSimpleName() + " obj = new " + model.getType().getSimpleName() + "();\n");
    writer.print("    Object val;\n");
    genFromSingleValued(model, writer);
    writer.print("    return obj;\n");
    writer.print("  }\n");
  }

  private static final EnumSet<PropertyKind> PK = EnumSet.of(PropertyKind.VALUE, PropertyKind.LIST, PropertyKind.SET);

  private void genFromSingleValued(DataObjectModel model, PrintWriter writer) {
    model
      .getPropertyMap()
      .values()
      .stream()
      .filter(prop -> PK.contains(prop.getKind()))
      .filter(PropertyInfo::isSetter)
      .forEach(prop -> {
        String rowType = rowType(prop.getType());
        switch (prop.getKind()) {
          case VALUE: {
            String meth = getter(prop.getType());
            if (meth != null) {
              bilto4(writer, meth, prop, wrapExpr(prop.getType(), "(" + rowType + ")val"));
            }
            break;
          }
          case LIST: {
            String meth = getArrayType(prop.getType());
            if (meth != null) {
              bilto4(writer, meth, prop, "java.util.Arrays.stream((" + rowType + "[])val).map(elt -> " + wrapExpr(prop.getType(), "elt") + ").collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new))");
            }
            break;
          }
          case SET: {
            String meth = getArrayType(prop.getType());
            if (meth != null) {
              bilto4(writer, meth, prop, "java.util.Arrays.stream((" + rowType + "[])val).map(elt -> " + wrapExpr(prop.getType(), "elt") + ").collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new))");
            }
            break;
          }
        }
      });
    model
      .getPropertyMap()
      .values()
      .stream()
      .filter(prop -> PK.contains(prop.getKind()))
      .filter(prop -> prop.isAdder() && !prop.isSetter())
      .forEach(prop -> {
        String meth = getArrayType(prop.getType());
        String rowType = rowType(prop.getType());
        if (meth != null) {
          writer.print("    val = row." + meth + "(\"" + prop.getName() + "\");\n");
          writer.print("    if (val != null) {\n");
          writer.print("      for (" + rowType + " elt : (" + rowType + "[])val) {\n");
          writer.print("        obj." + prop.getAdderMethod() + "(" + wrapExpr(prop.getType(), "elt") + ");\n");
          writer.print("      }\n");
          writer.print("    }\n");
        }
      });
  }

  private void bilto4(PrintWriter writer, String meth, PropertyInfo prop, String converter) {
    writer.print("    val = row." + meth + "(\"" + prop.getName() + "\");\n");
    writer.print("    if (val != null) {\n");
    writer.print("      obj." + prop.getSetterMethod() + "(" + converter +  ");\n");
    writer.print("    }\n");
  }

  private static String wrapExpr(TypeInfo type, String expr) {
    DataObjectInfo dataObject = type.getDataObject();
    if (dataObject != null) {
      MapperInfo deserializer = dataObject.getDeserializer();
      if (deserializer != null) {
        if (deserializer.getKind() == MapperKind.SELF) {
          return "new " + type.getName() + "(" + expr + ")";
        } else {
          return deserializer.getQualifiedName() + "." + String.join(".", deserializer
            .getSelectors()) + "(" + expr + ")";
        }
      }
      throw new UnsupportedOperationException();
    } else {
      return expr;
    }
  }

  private static String rowType(TypeInfo type) {
    DataObjectInfo dataObject = type.getDataObject();
    if (dataObject != null) {
      return rowType(dataObject.getJsonType());
    }
    return type.getName();
  }

  private static String getter(TypeInfo type) {
    switch (type.getKind()) {
      case PRIMITIVE:
        PrimitiveTypeInfo pt = (PrimitiveTypeInfo) type;
        return getter(pt.getBoxed());
      case BOXED_PRIMITIVE:
        return "get" + type.getSimpleName();
      case STRING:
        return "getString";
      case JSON_OBJECT:
        return "getJsonObject";
      case JSON_ARRAY:
        return "getJsonArray";
    }
    if (type instanceof ClassTypeInfo) {
      ClassTypeInfo ct = (ClassTypeInfo) type;
      switch (ct.getName()) {
        case "java.time.LocalDateTime":
          return "getLocalDateTime";
        case "java.time.LocalDate":
          return "getLocalDate";
        case "java.time.LocalTime":
          return "getLocalTime";
        case "java.time.OffsetTime":
          return "getOffsetTime";
        case "java.time.OffsetDateTime":
          return "getOffsetDateTime";
        case "java.time.temporal.Temporal":
          return "getTemporal";
        case "java.util.UUID":
          return "getUUID";
        case "io.vertx.core.buffer.Buffer":
          return "getBuffer";
      }
      DataObjectInfo dataObject = type.getDataObject();
      if (dataObject != null) {
        return getter(dataObject.getJsonType());
      }
    }
    return null;
  }

  private static String getArrayType(TypeInfo type) {
    String s = getter(type);
    if (s != null) {
      s += "Array";
    }
    return s;
  }

  private void genToParams(String visibility, DataObjectModel model, PrintWriter writer) {
    writer.print("\n");
    writer.print("  " + visibility + " static java.util.Map<String, Object> toParams(" + model.getType().getSimpleName() + " obj) {\n");
    writer.print("    java.util.Map<String, Object> params = new java.util.HashMap<>();\n");
    model
      .getPropertyMap()
      .values()
      .stream()
      .filter(prop -> PK.contains(prop.getKind()))
      .forEach(pi -> {
        AnnotationValueInfo ann = pi.getAnnotation(TemplateParam.class.getName());
        if (ann != null) {
          writer.print("    params.put(\"" + ann.getMember("value") + "\", obj." + pi.getGetterMethod() + "());\n");
        }
      });
    writer.print("    return params;\n");
    writer.print("  }\n");
  }
}
