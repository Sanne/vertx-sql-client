package io.vertx.sqlclient.template.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.template.SqlTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SqlTemplateImpl<R> implements SqlTemplate<R> {

  private static Pattern P = Pattern.compile(":(\\p{Alnum}+)");

  private final Pool client;
  private final TupleMapper paramMapper;
  private Function<PreparedQuery<RowSet<Row>>, PreparedQuery<R>> foobar;

  public SqlTemplateImpl(Pool client, Function<PreparedQuery<RowSet<Row>>, PreparedQuery<R>> foobar, String template) {
    this.client = client;
    this.foobar = foobar;
    this.paramMapper = new TupleMapper(client, template);
  }

  public SqlTemplateImpl(Pool client, Function<PreparedQuery<RowSet<Row>>, PreparedQuery<R>> foobar, TupleMapper paramMapper) {
    this.client = client;
    this.foobar = foobar;
    this.paramMapper = paramMapper;
  }

  @Override
  public <U> SqlTemplate<SqlResult<U>> collecting(Collector<Row, ?, U> collector) {
    return new SqlTemplateImpl<>(client, query -> query.collecting(collector), paramMapper);  }

  @Override
  public <U> SqlTemplate<RowSet<U>> mapping(Class<U> type) {
    return mapping(row -> {
      JsonObject json = new JsonObject();
      for (int i = 0;i < row.size();i++) {
        json.getMap().put(row.getColumnName(i), row.getValue(i));
      }
      return json.mapTo(type);
    });
  }

  @Override
  public <U> SqlTemplate<RowSet<U>> mapping(Function<Row, U> mapper) {
    return new SqlTemplateImpl<>(client, query -> query.mapping(mapper), paramMapper);
  }

  @Override
  public void execute(Object params, Handler<AsyncResult<R>> handler) {
    execute(JsonObject.mapFrom(params).getMap(), handler);
  }

  @Override
  public void execute(Map<String, Object> params, Handler<AsyncResult<R>> handler) {
    client.getConnection(ar1 -> {
      if (ar1.succeeded()) {
        SqlConnection conn = ar1.result();
        conn.prepare(paramMapper.sql, ar2 -> {
          if (ar2.succeeded()) {
            PreparedQuery<RowSet<Row>> query = ar2.result();
            PreparedQuery<R> apply = foobar.apply(query);
            apply.execute(paramMapper.mapTuple(params), ar3 -> {
              apply.close();
              conn.close();
              handler.handle(ar3);
            });
          } else {
            conn.close();
            handler.handle(Future.failedFuture(ar2.cause()));
          }
        });
      } else {
        handler.handle(Future.failedFuture(ar1.cause()));
      }
    });
  }

  @Override
  public void execute(List<Map<String, Object>> batch, Handler<AsyncResult<R>> handler) {
    client.getConnection(ar1 -> {
      if (ar1.succeeded()) {
        SqlConnection conn = ar1.result();
        conn.prepare(paramMapper.sql, ar2 -> {
          if (ar2.succeeded()) {
            PreparedQuery<RowSet<Row>> query = ar2.result();
            PreparedQuery<R> apply = foobar.apply(query);
            apply.batch(batch
              .stream()
              .map(paramMapper::mapTuple)
              .collect(Collectors.toList()), ar3 -> {
              apply.close();
              conn.close();
              handler.handle(ar3);
            });
          } else {
            conn.close();
            handler.handle(Future.failedFuture(ar2.cause()));
          }
        });
      } else {
        handler.handle(Future.failedFuture(ar1.cause()));
      }
    });
  }
}
