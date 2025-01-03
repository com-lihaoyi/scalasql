package scalasql
import scalasql.api.{TransactionTests, DbApiTests}
import operations.{
  ExprBooleanOpsTests,
  ExprNumericOpsTests,
  ExprAggNumericOpsTests,
  ExprAggOpsTests,
  ExprOpsTests,
  DbApiOpsTests,
  ExprStringOpsTests,
  ExprBlobOpsTests,
  ExprMathOpsTests
}
import query.{
  InsertTests,
  DeleteTests,
  SelectTests,
  JoinTests,
  FlatJoinTests,
  CompoundSelectTests,
  SubQueryTests,
  UpdateTests,
  UpdateJoinTests,
  UpdateSubQueryTests,
  ReturningTests,
  OnConflictTests,
  ValuesTests,
  LateralJoinTests,
  WindowFunctionTests,
  GetGeneratedKeysTests,
  WithCteTests,
  SchemaTests
}
import scalasql.dialects.{
  MySqlDialectTests,
  PostgresDialectTests,
  SqliteDialectTests,
  H2DialectTests,
  MsSqlDialectTests
}

package postgres {

  import utils.PostgresSuite

  object DbApiTests extends DbApiTests with PostgresSuite
  object TransactionTests extends TransactionTests with PostgresSuite

  object SelectTests extends SelectTests with PostgresSuite
  object JoinTests extends JoinTests with PostgresSuite
  object FlatJoinTests extends FlatJoinTests with PostgresSuite
  object InsertTests extends InsertTests with PostgresSuite
  object UpdateTests extends UpdateTests with PostgresSuite
  object DeleteTests extends DeleteTests with PostgresSuite
  object CompoundSelectTests extends CompoundSelectTests with PostgresSuite
  object UpdateJoinTests extends UpdateJoinTests with PostgresSuite
  object UpdateSubQueryTests extends UpdateSubQueryTests with PostgresSuite
  object ReturningTests extends ReturningTests with PostgresSuite
  object OnConflictTests extends OnConflictTests with PostgresSuite
  object ValuesTests extends ValuesTests with PostgresSuite
  object LateralJoinTests extends LateralJoinTests with PostgresSuite
  object WindowFunctionTests extends WindowFunctionTests with PostgresSuite
  object GetGeneratedKeysTests extends GetGeneratedKeysTests with PostgresSuite
  object SchemaTests extends SchemaTests with PostgresSuite

  object SubQueryTests extends SubQueryTests with PostgresSuite
  object WithCteTests extends WithCteTests with PostgresSuite

  object DbApiOpsTests extends DbApiOpsTests with PostgresSuite
  object ExprOpsTests extends ExprOpsTests with PostgresSuite
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with PostgresSuite
  object ExprNumericOpsTests extends ExprNumericOpsTests with PostgresSuite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with PostgresSuite
  object ExprSeqOpsTests extends ExprAggOpsTests with PostgresSuite
  object ExprStringOpsTests extends ExprStringOpsTests with PostgresSuite
  object ExprBlobOpsTests extends ExprBlobOpsTests with PostgresSuite
  object ExprMathOpsTests extends ExprMathOpsTests with PostgresSuite

  object DataTypesTests extends datatypes.DataTypesTests with PostgresSuite

  object OptionalTests extends datatypes.OptionalTests with PostgresSuite

  object PostgresDialectTests extends PostgresDialectTests

}

package hikari {

  import utils.HikariSuite

  object DbApiTests extends DbApiTests with HikariSuite
  object TransactionTests extends TransactionTests with HikariSuite

  object SelectTests extends SelectTests with HikariSuite
  object JoinTests extends JoinTests with HikariSuite
  object FlatJoinTests extends FlatJoinTests with HikariSuite
  object InsertTests extends InsertTests with HikariSuite
  object UpdateTests extends UpdateTests with HikariSuite
  object DeleteTests extends DeleteTests with HikariSuite
  object CompoundSelectTests extends CompoundSelectTests with HikariSuite
  object UpdateJoinTests extends UpdateJoinTests with HikariSuite
  object UpdateSubQueryTests extends UpdateSubQueryTests with HikariSuite
  object ReturningTests extends ReturningTests with HikariSuite
  object OnConflictTests extends OnConflictTests with HikariSuite
  object ValuesTests extends ValuesTests with HikariSuite
  object LateralJoinTests extends LateralJoinTests with HikariSuite
  object WindowFunctionTests extends WindowFunctionTests with HikariSuite
  object GetGeneratedKeysTests extends GetGeneratedKeysTests with HikariSuite
  object SchemaTests extends SchemaTests with HikariSuite

  object SubQueryTests extends SubQueryTests with HikariSuite
  object WithCteTests extends WithCteTests with HikariSuite

  object DbApiOpsTests extends DbApiOpsTests with HikariSuite
  object ExprOpsTests extends ExprOpsTests with HikariSuite
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with HikariSuite
  object ExprNumericOpsTests extends ExprNumericOpsTests with HikariSuite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with HikariSuite
  object ExprSeqOpsTests extends ExprAggOpsTests with HikariSuite
  object ExprStringOpsTests extends ExprStringOpsTests with HikariSuite
  object ExprBlobOpsTests extends ExprBlobOpsTests with HikariSuite
  object ExprMathOpsTests extends ExprMathOpsTests with HikariSuite

  object DataTypesTests extends datatypes.DataTypesTests with HikariSuite

  object OptionalTests extends datatypes.OptionalTests with HikariSuite

  object PostgresDialectTests extends PostgresDialectTests

}

package mysql {

  import utils.MySqlSuite

  object DbApiTests extends DbApiTests with MySqlSuite
  object TransactionTests extends TransactionTests with MySqlSuite

  object SelectTests extends SelectTests with MySqlSuite
  object JoinTests extends JoinTests with MySqlSuite
  object FlatJoinTests extends FlatJoinTests with MySqlSuite
  object InsertTests extends InsertTests with MySqlSuite
  object UpdateTests extends UpdateTests with MySqlSuite
  object DeleteTests extends DeleteTests with MySqlSuite
  object CompoundSelectTests extends CompoundSelectTests with MySqlSuite
  object UpdateJoinTests extends UpdateJoinTests with MySqlSuite
  // MySql does not support updates with subqueries referencing same table
  // object UpdateSubQueryTests extends UpdateSubQueryTests with MySqlSuite
  // MySql does not support INSERT/UPDATE RETURNING
  // object ReturningTests extends ReturningTests with MySqlSuite
  // MySql does not support onConflictIgnore and onConflictUpdate does not take columns
  // object OnConflictTests extends OnConflictTests with MySqlSuite
  object ValuesTests extends ValuesTests with MySqlSuite
  object LateralJoinTests extends LateralJoinTests with MySqlSuite
  object WindowFunctionTests extends WindowFunctionTests with MySqlSuite
  object GetGeneratedKeysTests extends GetGeneratedKeysTests with MySqlSuite

  object SubQueryTests extends SubQueryTests with MySqlSuite
  object WithCteTests extends WithCteTests with MySqlSuite

  object DbApiOpsTests extends DbApiOpsTests with MySqlSuite
  object ExprOpsTests extends ExprOpsTests with MySqlSuite
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with MySqlSuite
  object ExprNumericOpsTests extends ExprNumericOpsTests with MySqlSuite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with MySqlSuite
  object ExprSeqOpsTests extends ExprAggOpsTests with MySqlSuite
  object ExprStringOpsTests extends ExprStringOpsTests with MySqlSuite
  object ExprBlobOpsTests extends ExprBlobOpsTests with MySqlSuite
  object ExprMathOpsTests extends ExprMathOpsTests with MySqlSuite
  // In MySql, schemas are databases and this requires special treatment not yet implemented here
  // object SchemaTests extends SchemaTests with MySqlSuite

  object DataTypesTests extends datatypes.DataTypesTests with MySqlSuite
  object OptionalTests extends datatypes.OptionalTests with MySqlSuite

  object MySqlDialectTests extends MySqlDialectTests
}

package sqlite {

  import utils.SqliteSuite

  object DbApiTests extends DbApiTests with SqliteSuite
  object TransactionTests extends TransactionTests with SqliteSuite

  object SelectTests extends SelectTests with SqliteSuite
  object JoinTests extends JoinTests with SqliteSuite
  object FlatJoinTests extends FlatJoinTests with SqliteSuite
  object InsertTests extends InsertTests with SqliteSuite
  object UpdateTests extends UpdateTests with SqliteSuite
  object DeleteTests extends DeleteTests with SqliteSuite
  object CompoundSelectTests extends CompoundSelectTests with SqliteSuite
  object UpdateJoinTests extends UpdateJoinTests with SqliteSuite
  object UpdateSubQueryTests extends UpdateSubQueryTests with SqliteSuite
  object ReturningTests extends ReturningTests with SqliteSuite
  object OnConflictTests extends OnConflictTests with SqliteSuite
  object ValuesTests extends ValuesTests with SqliteSuite
  // Sqlite does not support lateral joins
  // object LateralJoinTests extends LateralJoinTests with SqliteSuite
  object WindowFunctionTests extends WindowFunctionTests with SqliteSuite
  // Sqlite does not support getGeneratedKeys https://github.com/xerial/sqlite-jdbc/issues/980
  // object GetGeneratedKeysTests extends GetGeneratedKeysTests with SqliteSuite

  object SubQueryTests extends SubQueryTests with SqliteSuite
  object WithCteTests extends WithCteTests with SqliteSuite

  object DbApiOpsTests extends DbApiOpsTests with SqliteSuite
  object ExprOpsTests extends ExprOpsTests with SqliteSuite
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with SqliteSuite
  object ExprNumericOpsTests extends ExprNumericOpsTests with SqliteSuite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with SqliteSuite
  object ExprSeqOpsTests extends ExprAggOpsTests with SqliteSuite
  object ExprStringOpsTests extends ExprStringOpsTests with SqliteSuite
  object ExprBlobOpsTests extends ExprBlobOpsTests with SqliteSuite
  // Sqlite doesn't support all these math operations
  // object ExprMathOpsTests extends ExprMathOpsTests with SqliteSuite
  // Sqlite doesn't support schemas
  // object SchemaTests extends SchemaTests with SqliteSuite

  object DataTypesTests extends datatypes.DataTypesTests with SqliteSuite
  object OptionalTests extends datatypes.OptionalTests with SqliteSuite

  object SqliteDialectTests extends SqliteDialectTests
}

package h2 {

  import utils.H2Suite

  object DbApiTests extends DbApiTests with H2Suite
  object TransactionTests extends TransactionTests with H2Suite

  object SelectTests extends SelectTests with H2Suite
  object JoinTests extends JoinTests with H2Suite
  object FlatJoinTests extends FlatJoinTests with H2Suite
  object InsertTests extends InsertTests with H2Suite
  object UpdateTests extends UpdateTests with H2Suite
  object DeleteTests extends DeleteTests with H2Suite
  object CompoundSelectTests extends CompoundSelectTests with H2Suite
  object UpdateJoinTests extends UpdateTests with H2Suite
  object UpdateSubQueryTests extends UpdateSubQueryTests with H2Suite
  // H2 does not support RETURNING keyword
  // object ReturningTests extends ReturningTests with H2Suite
  // H2 does not support ON CONFLICT IGNORE unless in postgres mode
  // object OnConflictTests extends OnConflictTests with H2Suite
  object ValuesTests extends ValuesTests with H2Suite
  // H2 does not support lateral joins
  // object LateralJoinTests extends LateralJoinTests with H2Suite
  object WindowFunctionTests extends WindowFunctionTests with H2Suite
  object GetGeneratedKeysTests extends GetGeneratedKeysTests with H2Suite
  object SchemaTests extends SchemaTests with H2Suite

  object SubQueryTests extends SubQueryTests with H2Suite
  object WithCteTests extends WithCteTests with H2Suite

  object DbApiOpsTests extends DbApiOpsTests with H2Suite
  object ExprOpsTests extends ExprOpsTests with H2Suite
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with H2Suite
  object ExprNumericOpsTests extends ExprNumericOpsTests with H2Suite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with H2Suite
  object ExprSeqOpsTests extends ExprAggOpsTests with H2Suite
  object ExprStringOpsTests extends ExprStringOpsTests with H2Suite
  object ExprBlobOpsTests extends ExprBlobOpsTests with H2Suite
  object ExprMathOpsTests extends ExprMathOpsTests with H2Suite

  object DataTypesTests extends datatypes.DataTypesTests with H2Suite
  object OptionalTests extends datatypes.OptionalTests with H2Suite

  object H2DialectTests extends H2DialectTests
}

package mssql {

  import utils.MsSqlSuite

  object DbApiTests extends DbApiTests with MsSqlSuite
  object TransactionTests extends TransactionTests with MsSqlSuite

  object SelectTests extends SelectTests with MsSqlSuite
  object JoinTests extends JoinTests with MsSqlSuite
  object FlatJoinTests extends FlatJoinTests with MsSqlSuite
  object InsertTests extends InsertTests with MsSqlSuite
  object UpdateTests extends UpdateTests with MsSqlSuite
  object DeleteTests extends DeleteTests with MsSqlSuite
  object CompoundSelectTests extends CompoundSelectTests with MsSqlSuite
  object UpdateJoinTests extends UpdateJoinTests with MsSqlSuite
  object UpdateSubQueryTests extends UpdateSubQueryTests with MsSqlSuite
  // object ReturningTests extends ReturningTests with MsSqlSuite
  // object OnConflictTests extends OnConflictTests with MsSqlSuite
  object ValuesTests extends ValuesTests with MsSqlSuite
  // object LateralJoinTests extends LateralJoinTests with MsSqlSuite
  object WindowFunctionTests extends WindowFunctionTests with MsSqlSuite
  object GetGeneratedKeysTests extends GetGeneratedKeysTests with MsSqlSuite
  object SchemaTests extends SchemaTests with MsSqlSuite

  object SubQueryTests extends SubQueryTests with MsSqlSuite
  object WithCteTests extends WithCteTests with MsSqlSuite

  object DbApiOpsTests extends DbApiOpsTests with MsSqlSuite
  object ExprOpsTests extends ExprOpsTests with MsSqlSuite
  //object ExprBooleanOpsTests extends ExprBooleanOpsTests with MsSqlSuite
  object ExprNumericOpsTests extends ExprNumericOpsTests with MsSqlSuite
  object ExprSeqNumericOpsTests extends ExprAggNumericOpsTests with MsSqlSuite
  object ExprSeqOpsTests extends ExprAggOpsTests with MsSqlSuite
  object ExprStringOpsTests extends ExprStringOpsTests with MsSqlSuite
  object ExprBlobOpsTests extends ExprBlobOpsTests with MsSqlSuite
  object ExprMathOpsTests extends ExprMathOpsTests with MsSqlSuite

  object DataTypesTests extends datatypes.DataTypesTests with MsSqlSuite

  object OptionalTests extends datatypes.OptionalTests with MsSqlSuite

  object MsSqlDialectTests extends MsSqlDialectTests

}
