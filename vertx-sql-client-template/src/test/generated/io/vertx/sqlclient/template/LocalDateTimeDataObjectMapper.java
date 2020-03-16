package io.vertx.sqlclient.template;

/**
 * Mapper for {@link LocalDateTimeDataObject}.
 * NOTE: This class has been automatically generated from the {@link LocalDateTimeDataObject} original class using Vert.x codegen.
 */
public class LocalDateTimeDataObjectMapper {

  public static final java.util.function.Function<io.vertx.sqlclient.Row, LocalDateTimeDataObject> READER = LocalDateTimeDataObjectMapper::fromRow;

  public static LocalDateTimeDataObject fromRow(io.vertx.sqlclient.Row row) {
    LocalDateTimeDataObject obj = new LocalDateTimeDataObject();
    Object val;
    val = row.getLocalDateTime("localDateTime");
    if (val != null) {
      obj.setLocalDateTime((java.time.LocalDateTime)val);
    }
    return obj;
  }

  public static java.util.Map<String, Object> toParams(LocalDateTimeDataObject obj) {
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    return params;
  }
}
