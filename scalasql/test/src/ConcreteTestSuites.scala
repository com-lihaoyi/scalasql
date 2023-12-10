package scalasql
import scalasql.api.{TransactionTests, DbApiTests}
import operations.{
  DbBooleanOpsTests,
  DbNumericOpsTests,
  DbAggNumericOpsTests,
  DbAggOpsTests,
  DbOpsTests,
  DbApiOpsTests,
  DbStringOpsTests,
  DbMathOpsTests
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
  WithCteTests
}
import scalasql.dialects.{
  MySqlDialectTests,
  PostgresDialectTests,
  SqliteDialectTests,
  H2DialectTests
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

  object SubQueryTests extends SubQueryTests with PostgresSuite
  object WithCteTests extends WithCteTests with PostgresSuite

  object DbApiOpsTests extends DbApiOpsTests with PostgresSuite
  object DbOpsTests extends DbOpsTests with PostgresSuite
  object DbBooleanOpsTests extends DbBooleanOpsTests with PostgresSuite
  object DbNumericOpsTests extends DbNumericOpsTests with PostgresSuite
  object DbSeqNumericOpsTests extends DbAggNumericOpsTests with PostgresSuite
  object DbSeqOpsTests extends DbAggOpsTests with PostgresSuite
  object DbStringOpsTests extends DbStringOpsTests with PostgresSuite
  object DbMathOpsTests extends DbMathOpsTests with PostgresSuite

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

  object SubQueryTests extends SubQueryTests with HikariSuite
  object WithCteTests extends WithCteTests with HikariSuite

  object DbApiOpsTests extends DbApiOpsTests with HikariSuite
  object DbOpsTests extends DbOpsTests with HikariSuite
  object DbBooleanOpsTests extends DbBooleanOpsTests with HikariSuite
  object DbNumericOpsTests extends DbNumericOpsTests with HikariSuite
  object DbSeqNumericOpsTests extends DbAggNumericOpsTests with HikariSuite
  object DbSeqOpsTests extends DbAggOpsTests with HikariSuite
  object DbStringOpsTests extends DbStringOpsTests with HikariSuite
  object DbMathOpsTests extends DbMathOpsTests with HikariSuite

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

  object SubQueryTests extends SubQueryTests with MySqlSuite
  object WithCteTests extends WithCteTests with MySqlSuite

  object DbApiOpsTests extends DbApiOpsTests with MySqlSuite
  object DbOpsTests extends DbOpsTests with MySqlSuite
  object DbBooleanOpsTests extends DbBooleanOpsTests with MySqlSuite
  object DbNumericOpsTests extends DbNumericOpsTests with MySqlSuite
  object DbSeqNumericOpsTests extends DbAggNumericOpsTests with MySqlSuite
  object DbSeqOpsTests extends DbAggOpsTests with MySqlSuite
  object DbStringOpsTests extends DbStringOpsTests with MySqlSuite
  object DbMathOpsTests extends DbMathOpsTests with MySqlSuite

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

  object SubQueryTests extends SubQueryTests with SqliteSuite
  object WithCteTests extends WithCteTests with SqliteSuite

  object DbApiOpsTests extends DbApiOpsTests with SqliteSuite
  object DbOpsTests extends DbOpsTests with SqliteSuite
  object DbBooleanOpsTests extends DbBooleanOpsTests with SqliteSuite
  object DbNumericOpsTests extends DbNumericOpsTests with SqliteSuite
  object DbSeqNumericOpsTests extends DbAggNumericOpsTests with SqliteSuite
  object DbSeqOpsTests extends DbAggOpsTests with SqliteSuite
  object DbStringOpsTests extends DbStringOpsTests with SqliteSuite
  // Sqlite doesn't support all these math operations
  // object DbMathOpsTests extends DbMathOpsTests with SqliteSuite

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

  object SubQueryTests extends SubQueryTests with H2Suite
  object WithCteTests extends WithCteTests with H2Suite

  object DbApiOpsTests extends DbApiOpsTests with H2Suite
  object DbOpsTests extends DbOpsTests with H2Suite
  object DbBooleanOpsTests extends DbBooleanOpsTests with H2Suite
  object DbNumericOpsTests extends DbNumericOpsTests with H2Suite
  object DbSeqNumericOpsTests extends DbAggNumericOpsTests with H2Suite
  object DbSeqOpsTests extends DbAggOpsTests with H2Suite
  object DbStringOpsTests extends DbStringOpsTests with H2Suite
  object DbMathOpsTests extends DbMathOpsTests with H2Suite

  object DataTypesTests extends datatypes.DataTypesTests with H2Suite
  object OptionalTests extends datatypes.OptionalTests with H2Suite

  object H2DialectTests extends H2DialectTests
}
