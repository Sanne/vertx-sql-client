/*
 * Copyright (C) 2017 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.vertx.sqlclient.template;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.unit.TestContext;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgTestBase;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class PgClientTest extends TemplateTestBase {

  protected Vertx vertx;
  protected Consumer<Handler<AsyncResult<PgConnection>>> connector;

  @Before
  public void setup() throws Exception {
    vertx = Vertx.vertx();
  }

  @After
  public void teardown(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  public PgClientTest() {
    connector = (handler) -> PgConnection.connect(vertx, connectOptions(), ar -> {
      handler.handle(ar.map(p -> p));
    });
  }

  @Test
  public void testQuery(TestContext ctx) {
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      QueryTemplate<Row> template = QueryTemplate.create(conn, "SELECT :id :: INT4 \"id\", :randomnumber :: INT4 \"randomnumber\"");
      Map<String, Object> params = new HashMap<>();
      params.put("id", 1);
      params.put("randomnumber", 10);
      template.query(params, ctx.asyncAssertSuccess(res -> {
        ctx.assertEquals(1, res.size());
        Row row = res.get(0);
        ctx.assertEquals(1, row.getInteger(0));
        ctx.assertEquals(10, row.getInteger(1));
      }));
    }));
  }

 @Test
  public void testQueryMap(TestContext ctx) {
    World w = new World();
    w.id = 1;
    w.randomnumber = 10;
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      QueryTemplate<World> template = QueryTemplate.create(conn, World.class, "SELECT :id :: INT4 \"id\", :randomnumber :: INT4 \"randomnumber\"");
      template.query(w, ctx.asyncAssertSuccess(res -> {
        ctx.assertEquals(1, res.size());
        World world = res.get(0);
        ctx.assertEquals(1, world.id);
        ctx.assertEquals(10, world.randomnumber);
      }));
    }));
  }

  @Test
  public void testLocalDateTimeWithJackson(TestContext ctx) {
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      LocalDateTime ldt = LocalDateTime.parse("2017-05-14T19:35:58.237666");
      QueryTemplate<LocalDateTimePojo> template = QueryTemplate.create(conn, LocalDateTimePojo.class, "SELECT :value :: TIMESTAMP WITHOUT TIME ZONE \"localDateTime\"");
      template.query(Collections.singletonMap("value", ldt), ctx.asyncAssertSuccess(result -> {
        ctx.assertEquals(1, result.size());
        ctx.assertEquals(ldt, result.get(0).localDateTime);
      }));
    }));
  }

  @Test
  public void testLocalDateTimeWithCodegen(TestContext ctx) {
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      LocalDateTime ldt = LocalDateTime.parse("2017-05-14T19:35:58.237666");
      QueryTemplate<LocalDateTimeDataObject> template = QueryTemplate.create(conn, LocalDateTimeDataObjectMapper.READER, "SELECT :value :: TIMESTAMP WITHOUT TIME ZONE \"localDateTime\"");
      template.query(Collections.singletonMap("value", ldt), ctx.asyncAssertSuccess(result -> {
        ctx.assertEquals(1, result.size());
        ctx.assertEquals(ldt, result.get(0).getLocalDateTime());
      }));
    }));
  }

  @Test
  public void testLocalDateTimeWithCodegenCollector(TestContext ctx) {
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      LocalDateTime ldt = LocalDateTime.parse("2017-05-14T19:35:58.237666");
      conn.preparedQuery(
        "select $1 :: TIMESTAMP WITHOUT TIME ZONE \"localDateTime\"",
        Tuple.of(ldt),
        LocalDateTimeDataObjectMapper.COLLECTOR,
        ctx.asyncAssertSuccess(res -> {
        ctx.assertEquals(1, res.size());
        ctx.assertEquals(1, res.value().size());
        ctx.assertEquals(ldt, res.value().get(0).getLocalDateTime());
      }));
    }));
  }


  /*
  @Test
  public void testBatchUpdate(TestContext ctx) {
    connector.accept(ctx.asyncAssertSuccess(conn -> {
      BatchTemplate<World> template = BatchTemplate.create(conn, World.class, "INSERT INTO World (id, randomnumber) VALUES (:id, :randomnumber)");
      template.batch(Arrays.asList(
        new World(20_000, 0),
        new World(20_001, 1),
        new World(20_002, 2),
        new World(20_003, 3)
      ), ctx.asyncAssertSuccess(v -> {
        conn.query("SELECT id, randomnumber from WORLD WHERE id=20000", ctx.asyncAssertSuccess(rowset -> {
          ctx.assertEquals(1, rowset.size());
          ctx.assertEquals(0, rowset.iterator().next().getInteger(1));
        }));
      }));
    }));
  }*/
}
