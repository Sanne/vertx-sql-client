package examples;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.template.BatchTemplate;
import io.vertx.sqlclient.template.QueryTemplate;
import io.vertx.sqlclient.template.annotations.Mapped;
import io.vertx.sqlclient.template.annotations.TemplateParam;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TemplateExamples {

  static class User {
    public long id;
    public String firstName;
    public String lastName;
  }

  public Function<Row, User> mapper = row -> {
    User user = new User();
    user.id = row.getInteger("id");
    user.firstName = row.getString("firstName");
    user.lastName = row.getString("lastName");
    return user;
  };


  public void fxExample(SqlClient client) {
    QueryTemplate<User> template = QueryTemplate.create(client, mapper, "SELECT * FROM users WHERE id=:id");
    template.query(Collections.singletonMap("id", 1))
      .onSuccess(users -> {
        users.forEach(user -> {
          System.out.println(user.firstName + " " + user.lastName);
        });
      });
  }

  public void bindingExample(SqlClient client) {
    QueryTemplate<User> template = QueryTemplate.create(client, User.class, "SELECT * FROM users WHERE id=:id");
    User u = new User();
    u.id = 1;
    template.query(u)
      .onSuccess(users -> {
        users.forEach(user -> {
          System.out.println(user.firstName + " " + user.lastName);
        });
      });
  }


  public void queryExampleWithDataObject(SqlClient client) {
    QueryTemplate<UserDataObject> template = QueryTemplate.create(client, UserDataObjectMapper.READER, "SELECT * FROM users WHERE id=:id");
    template.query(Collections.singletonMap("id", 1))
      .onSuccess(users -> {
        users.forEach(user -> {
          System.out.println(user.firstName + " " + user.lastName);
        });
      });
  }

  public void insertExampleWithDataObject(SqlClient client, UserDataObject user) {
    String sql = "INSERT INTO users (id, firstName, lastName) VALUES (':id', ':first_name', ':last_name')";
    QueryTemplate<Row> template = QueryTemplate.create(client, sql);
    template.query(user, UserDataObjectMapper::toParams)
      .onSuccess(res -> {
      });
  }

  @DataObject
  @Mapped
  public static class UserDataObject {

    private long id;
    private String firstName;
    private String lastName;

    public long getId() {
      return id;
    }

    @TemplateParam("id")
    public void setId(long id) {
      this.id = id;
    }

    public String getFirstName() {
      return firstName;
    }

    @TemplateParam("first_name")
    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    @TemplateParam("last_name")
    public void setLastName(String lastName) {
      this.lastName = lastName;
    }
  }

  /**
   * Mapper for {@link UserDataObject}.
   * NOTE: This class has been automatically generated from the {@link UserDataObject} original class using Vert.x codegen.
   */
  public static class UserDataObjectMapper {

    public static final java.util.function.Function<io.vertx.sqlclient.Row, UserDataObject> READER = UserDataObjectMapper::fromRow;

    public static UserDataObject fromRow(io.vertx.sqlclient.Row row) {
      UserDataObject obj = new UserDataObject();
      Object val;
      val = row.getString("firstName");
      if (val != null) {
        obj.setFirstName((java.lang.String)val);
      }
      val = row.getLong("id");
      if (val != null) {
        obj.setId((long)val);
      }
      val = row.getString("lastName");
      if (val != null) {
        obj.setLastName((java.lang.String)val);
      }
      return obj;
    }

    public static java.util.Map<String, Object> toParams(UserDataObject obj) {
      java.util.Map<String, Object> params = new java.util.HashMap<>();
      params.put("first_name", obj.getFirstName());
      params.put("id", obj.getId());
      params.put("last_name", obj.getLastName());
      return params;
    }
  }
}
