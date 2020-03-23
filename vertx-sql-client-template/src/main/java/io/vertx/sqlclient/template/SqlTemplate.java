package io.vertx.sqlclient.template;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.template.impl.SqlTemplateImpl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

@VertxGen
public interface SqlTemplate<R> {

  static SqlTemplate<RowSet<Row>> forQuery(Pool client, String template) {
    return new SqlTemplateImpl<>(client, Function.identity(), template);
  }

  @GenIgnore
  <U> SqlTemplate<SqlResult<U>> collecting(Collector<Row, ?, U> collector);

  <U> SqlTemplate<RowSet<U>> mapping(Class<U> type);

  <U> SqlTemplate<RowSet<U>> mapping(Function<Row, U> mapper);

  // Single
  void execute(Map<String, Object> params, Handler<AsyncResult<R>> handler);

  // Single
  @GenIgnore
  void execute(Object params, Handler<AsyncResult<R>> handler);

  // Batch
  @GenIgnore
  void execute(List<Map<String, Object>> batch, Handler<AsyncResult<R>> handler);

}
