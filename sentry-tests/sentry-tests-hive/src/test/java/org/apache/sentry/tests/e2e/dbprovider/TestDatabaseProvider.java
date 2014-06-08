/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sentry.tests.e2e.dbprovider;

import org.apache.sentry.provider.db.SentryAccessDeniedException;
import org.apache.sentry.provider.db.SentryAlreadyExistsException;
import org.apache.sentry.provider.db.SentryNoSuchObjectException;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestDatabaseProvider extends AbstractTestWithDbProvider {

  @Before
  public void setup() throws Exception {
    createContext();
  }

  /**
   * This test is only used for manual testing of beeline with Sentry Service
   * @throws Exception
   */
  @Ignore
  @Test
  public void beelineTest() throws Exception{
    while(true) {}
  }

  @Test
  public void testBasic() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE admin_role");
    statement.execute("GRANT ALL ON DATABASE default TO ROLE admin_role");
    statement.execute("GRANT ROLE admin_role TO GROUP " + ADMINGROUP);
    statement.execute("CREATE TABLE t1 (c1 string)");
    statement.execute("CREATE ROLE user_role");
    statement.execute("GRANT SELECT ON TABLE t1 TO ROLE user_role");
    statement.execute("GRANT ROLE user_role TO GROUP " + USERGROUP1);
    statement.close();
    connection.close();
    connection = context.createConnection(USER1_1);
    statement = context.createStatement(connection);
    context.assertSentryException(statement, "CREATE ROLE r2",
        SentryAccessDeniedException.class.getSimpleName());
    // test default of ALL
    statement.execute("SELECT * FROM t1");
    // test a specific role
    statement.execute("SET ROLE user_role");
    statement.execute("SELECT * FROM t1");
    // test NONE
    statement.execute("SET ROLE NONE");
    context.assertAuthzException(statement, "SELECT * FROM t1");
    // test ALL
    statement.execute("SET ROLE ALL");
    statement.execute("SELECT * FROM t1");
    statement.close();
    connection.close();
  }


  /**
   * Grant/Revoke privilege - Positive cases
   * @throws Exception
   */
  @Test
  public void testGrantRevokePrivileges() throws Exception {
    Connection connection;
    Statement statement;
    ResultSet resultSet;

    connection = context.createConnection(ADMIN1);
    statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");

    //Grant/Revoke All on server by admin
    statement.execute("GRANT ALL ON SERVER server1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("*"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.execute("REVOKE ALL ON SERVER server1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Grant/Revoke All on database by admin
    statement.execute("GRANT ALL ON DATABASE default to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.execute("REVOKE ALL ON DATABASE default from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Grant/Revoke All on URI by admin
    statement.execute("GRANT ALL ON URI 'file:///path' to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("file://path"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }
    statement.execute("REVOKE ALL ON URI 'file:///path' from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Grant/Revoke All on table by admin
    statement.execute("GRANT ALL ON TABLE tab1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("tab1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.execute("REVOKE ALL ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Grant/Revoke INSERT on table by admin
    statement.execute("GRANT INSERT ON TABLE tab1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("tab1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("insert"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.execute("REVOKE INSERT ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Grant/Revoke SELECT on table by admin
    statement.execute("GRANT SELECT ON TABLE tab1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("tab1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("select"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.execute("REVOKE SELECT ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);

    //Revoke Partial privilege on table by admin
    statement.execute("GRANT ALL ON TABLE tab1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    statement.execute("REVOKE INSERT ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("tab1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("select"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    //Revoke Partial privilege on table by admin
    statement.execute("GRANT ALL ON TABLE tab1 to role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    statement.execute("REVOKE SELECT ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while(resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("tab1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("insert"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor

    }

    statement.close();
    connection.close();
  }

  private void assertResultSize(ResultSet resultSet, int expected) throws SQLException{
    int count = 0;
    while(resultSet.next()) {
      count++;
    }
    assertThat(count, is(expected));
  }

  /**
   * Create and Drop role by admin
   * @throws Exception
   */
  @Test
  public void testCreateDropRole() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    ResultSet resultSet = statement.executeQuery("SHOW roles");
    assertResultSize(resultSet, 1);
    statement.execute("DROP ROLE role1");
    resultSet = statement.executeQuery("SHOW roles");
    assertResultSize(resultSet, 0);
  }

  /**
   * Corner cases
   * @throws Exception
   */
  @Test
  public void testCornerCases() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);

    //Drop a role which does not exist
    context.assertSentryException(statement, "DROP ROLE role1",
        SentryNoSuchObjectException.class.getSimpleName());

    //Create a role which already exists
    statement.execute("CREATE ROLE role1");
    context.assertSentryException(statement, "CREATE ROLE role1",
        SentryAlreadyExistsException.class.getSimpleName());

    //Drop role when privileges mapping exists and create role with same name, old mappings should not exist
    //state: role1
    statement.execute("GRANT ROLE role1 TO GROUP " + USERGROUP1);
    statement.execute("GRANT ALL ON SERVER server1 TO ROLE role1");
    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    statement.execute("DROP ROLE role1");
    statement.execute("CREATE ROLE role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);


    //Grant role, when role doesn't exist
    //state: role1
    context.assertSentryException(statement, "GRANT role role2 TO GROUP " + USERGROUP1,
        SentryNoSuchObjectException.class.getSimpleName());


    //Grant multiple roles to a group
    //state: role1
    statement.execute("CREATE ROLE role2");
    statement.execute("GRANT ROLE role2 to GROUP " + USERGROUP1);
    statement.execute("GRANT ROLE role1 to GROUP " + USERGROUP1);
    resultSet = statement.executeQuery("SHOW ROLE GRANT GROUP " + USERGROUP1);
    assertResultSize(resultSet, 2);

    //Grant role when mapping exists
    //state: role1, role2 -> usergroup1
    statement.execute("GRANT ROLE role1 to GROUP " + USERGROUP1);

    //Revoke role after role has been dropped
    //state: role1, role2 -> usergroup1
    statement.execute("DROP ROLE role2");
    context.assertSentryException(statement, "REVOKE role role2 from group " + USERGROUP1,
        SentryNoSuchObjectException.class.getSimpleName());

    //Revoke role from a group when mapping doesnt exist
    //state: role1 -> usergroup1
    statement.execute("REVOKE ROLE role1 from GROUP " + USERGROUP1);
    statement.execute("REVOKE ROLE role1 from GROUP " + USERGROUP1);

    //Grant privilege to a role, privilege already exists, mapping already exists
    //state: role1
    //TODO: Remove this comment SENTRY-181
    statement.execute("CREATE ROLE role2");
    statement.execute("GRANT ALL ON SERVER server1 TO ROLE role1");
    statement.execute("GRANT ALL ON SERVER server1 TO ROLE role1");
    statement.execute("GRANT ALL ON SERVER server1 TO ROLE role2");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    resultSet = statement.executeQuery("SHOW GRANT ROLE role2");
    assertResultSize(resultSet, 1);

    //Multiple privileges to a role
    //state: role1,role2 -> grant all on server
    statement.execute("GRANT ALL ON TABLE tab1 to ROLE role2");
    statement.execute("GRANT ALL,INSERT ON TABLE tab1 to ROLE role2");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role2");
    assertResultSize(resultSet, 3);
    statement.execute("DROP role role2");

    //Revoke privilege when privilege doesnt exist
    //state: role1 -> grant all on server
    statement.execute("REVOKE ALL ON TABLE tab1 from role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    int count = 0;
    //Verify we still have all on server
    while(resultSet.next()) {
      count++;
      assertThat(resultSet.getString(1), equalToIgnoringCase("*"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));

    }
    assertThat(count, is(1));

    //Revoke privilege, when role doesnt exist
    //state: role1 -> grant all on server
    context.assertSentryException(statement, "REVOKE ALL ON SERVER server1 from role role2",
        SentryNoSuchObjectException.class.getSimpleName());

    //Revoke privilege when privilege exists but mapping doesnt exist
    //state: role1 -> grant all on server
    statement.execute("CREATE ROLE role2");
    statement.execute("GRANT ALL on TABLE tab1 to ROLE role2");
    statement.execute("REVOKE ALL on TABLE tab1 from Role role1");
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
  }
  /**
   * SHOW ROLES
   * @throws Exception
   */
  @Test
  public void testShowRoles() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    ResultSet resultSet = statement.executeQuery("SHOW ROLES");
    assertResultSize(resultSet, 0);
    statement.execute("CREATE ROLE role1");
    statement.execute("CREATE ROLE role2");
    resultSet = statement.executeQuery("SHOW ROLES");
    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
    assertThat(resultSetMetaData.getColumnCount(), is(1));
    assertThat(resultSetMetaData.getColumnName(1), equalToIgnoringCase("role"));

    Set<String> roles = new HashSet<String>();
    while ( resultSet.next()) {
      roles.add(resultSet.getString(1));
    }
    assertThat(roles.size(), is(2));
    assertTrue(roles.contains("role1"));
    assertTrue(roles.contains("role2"));
    statement.close();
    connection.close();
  }

  /**
   * SHOW ROLE GRANT GROUP groupName
   * @throws Exception
   */
  @Test
  public void testShowRolesByGroup() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    context.assertSentryException(statement,"SHOW ROLE GRANT GROUP " + ADMINGROUP,
        SentryNoSuchObjectException.class.getSimpleName());
    statement.execute("CREATE ROLE role1");
    statement.execute("CREATE ROLE role2");
    statement.execute("CREATE ROLE role3");
    statement.execute("GRANT ROLE role1 to GROUP " + ADMINGROUP);

    ResultSet resultSet = statement.executeQuery("SHOW ROLE GRANT GROUP " + ADMINGROUP);
    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
    assertThat(resultSetMetaData.getColumnCount(), is(4));
    assertThat(resultSetMetaData.getColumnName(1), equalToIgnoringCase("role"));
    assertThat(resultSetMetaData.getColumnName(2), equalToIgnoringCase("grant_option"));
    assertThat(resultSetMetaData.getColumnName(3), equalToIgnoringCase("grant_time"));
    assertThat(resultSetMetaData.getColumnName(4), equalToIgnoringCase("grantor"));
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("role1"));
      assertThat(resultSet.getBoolean(2), is(new Boolean("False")));
      //Create time is not tested
      //assertThat(resultSet.getLong(3), is(new Long(0)));
      assertThat(resultSet.getString(4), equalToIgnoringCase(ADMIN1));
    }
    statement.close();
    connection.close();
  }

  /**
   * SHOW GRANT ROLE roleName
   * @throws Exception
   */
  @Test
  public void testShowPrivilegesByRole() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 0);
    statement.execute("CREATE ROLE role2");
    statement.execute("GRANT SELECT ON TABLE t1 TO ROLE role1");

    resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
    //| database  | table  | partition  | column  | principal_name  |
    // principal_type | privilege  | grant_option  | grant_time  | grantor  |
    assertThat(resultSetMetaData.getColumnCount(), is(10));
    assertThat(resultSetMetaData.getColumnName(1), equalToIgnoringCase("database"));
    assertThat(resultSetMetaData.getColumnName(2), equalToIgnoringCase("table"));
    assertThat(resultSetMetaData.getColumnName(3), equalToIgnoringCase("partition"));
    assertThat(resultSetMetaData.getColumnName(4), equalToIgnoringCase("column"));
    assertThat(resultSetMetaData.getColumnName(5), equalToIgnoringCase("principal_name"));
    assertThat(resultSetMetaData.getColumnName(6), equalToIgnoringCase("principal_type"));
    assertThat(resultSetMetaData.getColumnName(7), equalToIgnoringCase("privilege"));
    assertThat(resultSetMetaData.getColumnName(8), equalToIgnoringCase("grant_option"));
    assertThat(resultSetMetaData.getColumnName(9), equalToIgnoringCase("grant_time"));
    assertThat(resultSetMetaData.getColumnName(10), equalToIgnoringCase("grantor"));

    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("t1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("select"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }
    statement.close();
    connection.close();
  }

  /**
   * SHOW GRANT ROLE roleName ON TABLE tableName
   * @throws Exception
   */
  @Test
  public void testShowPrivilegesByRoleOnObjectGivenTable() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("GRANT SELECT ON TABLE t1 TO ROLE role1");

    //On table - positive
    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON TABLE t1");
    int rowCount = 0 ;
    while ( resultSet.next()) {
      rowCount++;
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase("t1"));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("select"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }
    assertThat(rowCount, is(1));
    //On table - negative
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON TABLE tab1");
    rowCount = 0 ;
    while (resultSet.next()) {
      rowCount++;
    }
    assertThat(rowCount, is(0));
    statement.close();
    connection.close();
  }

    /**
     * SHOW GRANT ROLE roleName ON TABLE tableName
     * @throws Exception
     */
  @Test
  public void testShowPrivilegesByRoleOnObjectGivenDatabase() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("GRANT ALL ON DATABASE default TO ROLE role1");

    //On Table - positive
    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON TABLE tab1");
    int rowCount = 0 ;
    while ( resultSet.next()) {
      rowCount++;
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    //On Database - positive
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON DATABASE default");
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("default"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));//table
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    //On Database - negative
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON DATABASE db1");
    rowCount = 0 ;
    while (resultSet.next()) {
      rowCount++;
    }
    assertThat(rowCount, is(0));
    statement.close();
    connection.close();
  }

  /**
   * SHOW GRANT ROLE roleName ON TABLE tableName
   * @throws Exception
   */
  @Test
  public void testShowPrivilegesByRoleObObjectGivenServer() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("GRANT ALL ON SERVER server1 TO ROLE role1");

    //On table - positive
    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON TABLE tab1");
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("*"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    //On Database - postive
    resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON DATABASE default");
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("*"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }

    statement.close();
    connection.close();
  }

  /**
   * SHOW GRANT ROLE roleName ON DATABASE dbName: Needs Hive patch
   * @throws Exception
   */
  @Ignore
  @Test
  public void testShowPrivilegesByRoleOnUri() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("GRANT ALL ON URI 'file:///tmp/file.txt' TO ROLE role1");

    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1 ON URI 'file:///tmp/file.txt'");
    assertTrue("Expecting SQL Exception", false);
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("file:///tmp/file.txt"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));//table
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }
    statement.close();
    connection.close();
  }

  /**
   * SHOW CURRENT ROLE
   * @throws Exception
   */
  @Test
  public void testShowCurrentRole() throws Exception {
    //TODO: Add more test cases once we fix SENTRY-268
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("SET ROLE role1");
    ResultSet resultSet = statement.executeQuery("SHOW CURRENT ROLES");
    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
    assertThat(resultSetMetaData.getColumnCount(), is(1));
    assertThat(resultSetMetaData.getColumnName(1), equalToIgnoringCase("role"));

    while( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("role1"));
    }
    statement.close();
    connection.close();
  }

  // See SENTRY-166
  @Test
  public void testUriWithEquals() throws Exception {
    Connection connection = context.createConnection(ADMIN1);
    Statement statement = context.createStatement(connection);
    statement.execute("CREATE ROLE role1");
    statement.execute("GRANT ALL ON URI 'file:///tmp/partition=value/file.txt' TO ROLE role1");

    ResultSet resultSet = statement.executeQuery("SHOW GRANT ROLE role1");
    assertResultSize(resultSet, 1);
    while ( resultSet.next()) {
      assertThat(resultSet.getString(1), equalToIgnoringCase("file:///tmp/partition=value/file.txt"));
      assertThat(resultSet.getString(2), equalToIgnoringCase(""));//table
      assertThat(resultSet.getString(3), equalToIgnoringCase(""));//partition
      assertThat(resultSet.getString(4), equalToIgnoringCase(""));//column
      assertThat(resultSet.getString(5), equalToIgnoringCase("role1"));//principalName
      assertThat(resultSet.getString(6), equalToIgnoringCase("role"));//principalType
      assertThat(resultSet.getString(7), equalToIgnoringCase("*"));
      assertThat(resultSet.getBoolean(8), is(new Boolean("False")));//grantOption
      //Create time is not tested
      //assertThat(resultSet.getLong(9), is(new Long(0)));
      assertThat(resultSet.getString(10), equalToIgnoringCase(ADMIN1));//grantor
    }
    statement.close();
    connection.close();
  }

}
