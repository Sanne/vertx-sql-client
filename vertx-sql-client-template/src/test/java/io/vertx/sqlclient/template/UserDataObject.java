package io.vertx.sqlclient.template;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.sqlclient.template.annotations.Mapped;
import io.vertx.sqlclient.template.annotations.TemplateParam;

@DataObject
@Mapped
public class UserDataObject {

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
